package compojar.bnf;

import com.squareup.javapoet.TypeName;
import compojar.gen.Namer;
import compojar.gen.ParserInfo;
import compojar.util.T2;
import compojar.util.Util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static compojar.bnf.Symbol.variable;
import static compojar.util.T2.t2;
import static compojar.util.Util.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * Performs left-factoring on a BNF, which results in elimination of all common prefixes.
 * <p>
 * The input BNF must not contain left-recursive rules.
 */
public class LeftFactoring {

    private final Namer namer;

    public LeftFactoring(final Namer namer) {
        this.namer = namer;
    }

    public Data apply(Data data) {
        return findFirstCommonPrefix(data.bnf)
                .map(cp -> removeCommonChainPrefix(cp, data.bnf).orElse(cp))
                .map(pref -> removeCommonPrefix(data.bnf, data.astMetadata, pref))
                .map(data_ -> new EmptyProductionElimination(namer).apply(data_.bnf(), data_.astMetadata())
                        .map(pair -> pair.map(Data::new))
                        .orElse(data_))
                .map(this::apply)
                .orElse(data);
    }

    private Data removeCommonPrefix(
            final BNF bnf,
            final AstMetadata astMetadata,
            final CommonPrefix commonPrefix)
    {
        final var startVar = first(commonPrefix.chains).getFirst();

        // Variable -> (maybe Rf, Rp)
        final Map<Variable, T2<Optional<Rule>, Rule>> rewritten =
                rewriteRules(commonPrefix.symbol,
                             commonPrefix.chains.stream()
                                     .map(ch -> subList(ch, 1))
                                     .flatMap(Collection::stream)
                                     .toList(),
                             bnf);
        final var ruleOVar = variable(namer.randomName("PREF"));
        final var ruleOkVar = variable(ruleOVar.name() + "_K");
        final Rule ruleO = new Derivation(ruleOVar, List.of(commonPrefix.symbol, ruleOkVar));
        final Rule ruleOk = new Selection(ruleOkVar, commonPrefix.chains.stream().map(ch -> ch.get(1))
                .distinct()
                .map(var -> requireKey(rewritten, var).snd().lhs())
                .toList());

        final Rule newStartRule = bnf.requireSelectionFor(startVar)
                .updateRhs(rhs -> prepend(ruleOVar, rhs.stream()
                        // Replace variables for rewritten rules:
                        // * If a rewritten rule has a full rule, use the full rule.
                        // * Otherwise, drop the variable from the RHS.
                        .map(v -> Optional.ofNullable(rewritten.get(v))
                                .map(pair -> pair.fst().map(Rule::lhs))
                                .orElse(Optional.of(v)))
                        .flatMap(Optional::stream)
                        .toList()));

        final Set<Rule> newRules = concatSet(
                Set.of(ruleO, ruleOk),
                bnf.rules().stream()
                        .flatMap(r -> Stream.concat(Stream.of(r),
                                                    Optional.ofNullable(rewritten.get(r.lhs()))
                                                            .map(pair -> pair.map((maybeFull, partial) -> Stream.concat(maybeFull.stream(), Stream.of(partial))))
                                                            .orElseGet(Stream::empty)))
                        .collect(toSet()));

        // If the common prefix symbol is a parameterised terminal, then its parameters should be propagated by partial parsers.
        final var parameters = switch (commonPrefix.symbol) {
            case Terminal t when t.hasParameters() -> t.getParameters();
            default -> List.<Parameter>of();
        };

        final var ruleO_parserInfo = astMetadata.requireParserInfo(startVar);
        final var ruleOk_parserInfo = new ParserInfo.PartialS(
                astMetadata.requireParserInfo(startVar).requireAstNodeName(),
                parameters.stream().map(p -> TypeName.get(p.type())).toList());

        final var rewritten_parserInfoMap = reduce(
                rewritten.keySet(),
                Map.<Variable, ParserInfo>of(),
                (acc, var) -> {
                    var pair = rewritten.get(var);
                    var varParserInfo = astMetadata.requireParserInfo(var);
                    Map<Variable, ParserInfo> addedParserInfos = new HashMap<>();

                    var rpParserInfo = withAddedParameters(bnf.requireRuleFor(var), varParserInfo, parameters);
                    addedParserInfos.put(pair.snd().lhs(), rpParserInfo);

                    pair.fst().ifPresent(rf -> addedParserInfos.put(rf.lhs(), varParserInfo));

                    return mapStrictMerge(acc, addedParserInfos);
                }
        );

        final var newBnf = new BNF(newRules, bnf.start())
                .updateRule(startVar, $ -> newStartRule);

        final var newAstMetadata = astMetadata.addParserInfos(
                mapStrictMerge(Map.of(ruleOVar, ruleO_parserInfo,
                                      ruleOkVar, ruleOk_parserInfo),
                               rewritten_parserInfoMap));

        return new Data(newBnf, newAstMetadata).removeUnused();
    }

    /**
     * @param rule
     * @param info  parser info associated with {@code rule}
     */
    private ParserInfo withAddedParameters(Rule rule, ParserInfo info, List<Parameter> parameters) {
        var paramTypeNames = parameters.stream().map(Parameter::type).map(TypeName::get).toList();
        var paramNames = parameters.stream().map(Parameter::name).map(CharSequence::toString).toList();
        var paramPairs = zip(paramTypeNames, paramNames).toList();
        return switch (info) {
            case ParserInfo.Full full -> switch (rule) {
                case Derivation $ -> new ParserInfo.PartialD(full.astNode(), paramPairs);
                case Selection $ -> new ParserInfo.PartialS(full.astNode(), paramTypeNames);
            };
            case ParserInfo.PartialD d -> new ParserInfo.PartialD(d.astNode(), paramPairs);
            case ParserInfo.PartialS s -> new ParserInfo.PartialS(s.astNode(), paramTypeNames);
            default -> throw new IllegalStateException(String.format("Unexpected parser info: %s", info));
        };
    }

    private Map<Variable, T2<Optional<Rule>, Rule>> rewriteRules(final Symbol prefix, final Collection<Variable> variables, final BNF bnf) {
        return reduce(variables.stream(),
                      Map.of(),
                      (map, var) -> rewriteRule(prefix, var, variables, map, bnf));
    }

    // Map associates original rules with a pair (optional new full parser rule, new partial parser rule)
    private Map<Variable, T2<Optional<Rule>, Rule>> rewriteRule(
            final Symbol prefix,
            final Variable variable,
            final Collection<Variable> allVariables,
            final Map<Variable, T2<Optional<Rule>, Rule>> acc,
            final BNF bnf)
    {
        final var rule = bnf.requireRuleFor(variable);
        if (acc.containsKey(rule.lhs())) {
            return acc;
        }
        else {
            return switch (rule) {
                case Derivation derivation -> {
                    final var firstRhsSym = rule.rhs().getFirst();
                    // Directly derives prefix.
                    if (firstRhsSym.equals(prefix)) {
                        yield insert(acc, rule.lhs(),
                                     t2(Optional.empty(), new Derivation(partialRuleVariable(derivation), subList(derivation.rhs(), 1))));
                    }
                    // Transitively derives prefix.
                    else {
                        final var y = (Variable) firstRhsSym;
                        final var acc_ = rewriteRule(prefix, y, allVariables, acc, bnf);
                        yield requireKey(acc_, y).map((maybeYf, yp) -> {
                            final var ruleP = new Derivation(partialRuleVariable(derivation),
                                                             replace(derivation.rhs(), 0, yp.lhs()));
                            final Optional<Rule> maybeRuleF = maybeYf.map(yf -> new Derivation(fullRuleVariable(derivation),
                                                                                               replace(derivation.rhs(), 0, yf.lhs())));
                            return insert(acc_, rule.lhs(), t2(maybeRuleF, ruleP));
                        });

                    }
                }
                case Selection selection -> {
                    final var acc_ = reduce(selection.rhs().stream().filter(allVariables::contains),
                                            acc,
                                            (_acc, option) -> rewriteRule(prefix, option, allVariables, _acc, bnf));
                    final var ruleP = new Selection(partialRuleVariable(selection),
                                                    selection.rhs().stream()
                                                            .map(v -> Optional.ofNullable(acc_.get(v)).map(T2::snd))
                                                            .flatMap(Optional::stream)
                                                            .map(Rule::lhs)
                                                            .toList());
                    final Optional<Rule> maybeRuleF = Optional.of(selection.rhs().stream().filter(v -> !allVariables.contains(v)).toList())
                            .filter(list -> !list.isEmpty())
                            .map(rhs -> new Selection(fullRuleVariable(selection), rhs));
                    yield insert(acc_, variable, t2(maybeRuleF, ruleP));
                }
            };
        }
    }

    private Variable partialRuleVariable(final Rule rule) {
        return variable(partialRuleName(rule.lhs()));
    }

    private String partialRuleName(final CharSequence name) {
        return "Partial_" + name;
    }

    private Variable fullRuleVariable(final Rule rule) {
        return variable(fullRuleName(rule.lhs()));
    }

    private String fullRuleName(final CharSequence name) {
        return "Full_" + name;
    }

    private Optional<CommonPrefix> findFirstCommonPrefix(BNF bnf) {
        return bnf.rules().stream()
                // Sort to enable determinism.
                .sorted(Rule.compareByLhs)
                .filter(r -> r instanceof Selection)
                .map(r -> (Selection) r)
                .map(r -> findFirstCommonPrefix(r, bnf))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<CommonPrefix> findFirstCommonPrefix(Selection selection, BNF bnf) {
        final var chains = allChains(selection, bnf).toList();

        // Compare all pairs with each other to find all existing common prefixes.
        // A group is a pair (prefix contents, all common prefixes with such contents)
        final var commonPrefixGroups = enumeratedStream(chains.stream(),
                                                        (chain, i) -> subList(chains, i + 1).stream()
                                                                .map(otherChain -> findCommonPrefix(chain, otherChain, bnf)
                                                                        .map(pref -> pref.equals(chain.getLast()) && pref.equals(otherChain.getLast())
                                                                                // Given prefix C and chains A -> C, A -> B -> C, drop C from the chains.
                                                                                ? new CommonPrefix(pref, List.of(dropRight(chain, 1), dropRight(otherChain, 1)))
                                                                                : new CommonPrefix(pref, List.of(chain, otherChain))))
                                                                .flatMap(Optional::stream))
                .flatMap(Function.identity())
                .collect(Collectors.groupingBy(CommonPrefix::symbol));
        // Take any group
        return commonPrefixGroups.isEmpty()
                ? Optional.empty()
                : Optional.of(CommonPrefix.combineChains(first(commonPrefixGroups.values())));
    }

    /**
     * Returns a stream of all chains that start from the specified rule and end at the first terminal (excluded).
     * Since a terminal can occur only in the RHS of a derivation, the last symbol in each chain is a derivation rule.
     */
    private Stream<List<Variable>> allChains(final Rule rule, final BNF bnf) {
        return switch (rule) {
            case Derivation derivation -> {
                if (derivation.rhs().isEmpty() || derivation.rhs().getFirst() instanceof Terminal) {
                    yield Stream.of(List.of(derivation.lhs()));
                }
                else yield allChains(bnf.requireRuleFor((Variable) derivation.rhs().getFirst()), bnf)
                        .map(chain -> prepend(derivation.lhs(), chain));
            }
            case Selection selection -> selection.rhs().stream()
                    .flatMap(v -> allChains(bnf.requireRuleFor(v), bnf))
                    .map(chain -> prepend(selection.lhs(), chain));
        };
    }

    private Optional<Symbol> findCommonPrefix(List<? extends Variable> xs, List<? extends Variable> ys, BNF bnf) {
        // The last rule in a chain is always a derivation.
        if (!(bnf.requireRuleFor(xs.getLast()) instanceof Derivation dx))
            throw new IllegalArgumentException("The last rule in chain [%s] is not a derivation.".formatted(String.join(", ", xs)));
        if (!(bnf.requireRuleFor(ys.getLast()) instanceof Derivation dy))
            throw new IllegalArgumentException("The last rule in chain [%s] is not a derivation.".formatted(String.join(", ", ys)));

        // if (dx.lhs().equals(dy.lhs()))
        //     return Optional.of(dx.lhs());
        if (optionalMap2(firstOpt(dx.rhs()), firstOpt(dy.rhs()), Objects::equals).orElse(false))
            return firstOpt(dx.rhs());
        else return Optional.empty();
    }

    /**
     * The result is present only if shortened chains begin with a selection.
     * <p>
     * For example, given chains [A -> B -> C -> D, A -> B -> C -> E], where C is a selection, the result is [C -> D, C -> E].
     */
    private Optional<CommonPrefix> removeCommonChainPrefix(CommonPrefix commonPrefix, BNF bnf) {
        int commonChainSize = (int) zipAll(commonPrefix.chains)
                .takeWhile(Util::allElementsEqual)
                .count();
        var commonChain = first(commonPrefix.chains).subList(0, commonChainSize);

        var idx = findWithIndex(commonChain.reversed(), var -> bnf.requireRuleFor(var) instanceof Selection)
                .map(T2::snd)
                .map(i -> commonChain.size() - 1 - i)
                .orElse(-1);

        if (idx < 0) {
            return Optional.empty();
        }
        else {
            var newChains = commonPrefix.chains.stream().map(ch -> subList(ch, idx)).toList();
            return Optional.of(new CommonPrefix(commonPrefix.symbol, newChains));
        }
    }

    /**
     * @param symbol  the common prefix.
     *                <p> If this is a non-terminal, then all chains lead to a single derivation associated with this non-terminal.
     *                <p> If this is a terminal, then all chains lead to different derivations whose RHS start with this terminal.
     * @param chains  the chains that lead to the common prefix.
     *                Each chain is at least 2 elements long.
     *                The first variable in each chain is the same and is the selection rule at the start.
     */
    record CommonPrefix(Symbol symbol, Collection<List<Variable>> chains) {

        static CommonPrefix combineChains(Collection<CommonPrefix> commonPrefixes) {
            if (commonPrefixes.isEmpty())
                throw new IllegalArgumentException("empty collection");
            if (!allElementsEqual(commonPrefixes, CommonPrefix::symbol)) {
                throw new IllegalArgumentException("Trying to combine chains with different common prefixes: [%s]"
                                                           .formatted(commonPrefixes.stream()
                                                                              .map(cp -> "[%s]".formatted(String.join(", ", cp.symbol())))
                                                                              .collect(joining(", "))));
            }
            return new CommonPrefix(first(commonPrefixes).symbol,
                                    commonPrefixes.stream().map(CommonPrefix::chains).flatMap(Collection::stream).distinct().toList());
        }

    }

    public record Data (BNF bnf, AstMetadata astMetadata) {
        public Data removeUnused() {
            return bnf.removeUnused()
                    .map((newBnf, unused) -> new Data(newBnf, astMetadata.removeVariables(unused)));
        }
    }

}
