package compojar.bnf;

import compojar.gen.Namer;
import compojar.gen.ParserInfo;
import compojar.util.JavaPoet;
import compojar.util.Util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static compojar.bnf.Derivation.derivation;
import static compojar.bnf.Symbol.variable;
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
                .map(pref -> apply(removeCommonPrefix(data.bnf, data.astMetadata, pref)))
                .orElse(data);
    }

    private Data removeCommonPrefix(
            final BNF bnf,
            final AstMetadata astMetadata,
            final CommonPrefix commonPrefix)
    {
        Variable startVar = first(commonPrefix.chains).getFirst();

        var newParserInfoMap = new HashMap<Variable, ParserInfo>(astMetadata.parserInfoMap());

        var newSelectionName = namer.randomName();
        // For each last variable in a chain, create a new derivation whose RHS doesn't contain the common prefix.
        var newDerivations = commonPrefix.chains.stream()
                .map(ch -> {
                    var lastVar = variable(newSelectionName + "_" + ch.getLast());
                    var oldRule = bnf.requireRuleFor(ch.getLast());
                    return switch (oldRule) {
                        case Derivation $ -> derivation(lastVar, subList(oldRule.rhs(), 1));
                        case Selection $ -> derivation(lastVar, List.of());
                    };
                })
                .toList();
        // New derivations are partial parsers of the same node as the derivations they replace.
        commonPrefix.chains.stream()
                .map(List::getLast)
                // The last var must be a full parser:
                // - It cannot be a bridge because that doesn't make sense.
                // - It cannot be a partial parser because rules corresponding to partial parsers cannot contain a common prefix.
                .forEach(lastVar -> {
                    final var parserInfo = (ParserInfo.Full) astMetadata.requireParserInfo(lastVar);
                    final ParserInfo newParserInfo;
                    if (bnf.requireRuleFor(lastVar) instanceof Selection) {
                        newParserInfo = new ParserInfo.PartialEmpty(namer.astNodeClassName((Variable) commonPrefix.symbol));
                    }
                    else {
                        // Translating the prefix into field names for AST node components.
                        final var components = Stream.of(commonPrefix.symbol)
                                .map(sym -> astMetadata.findAstNodeField(parserInfo.astNode(), sym))
                                // A symbol may not necessarily be represented in the AST node (e.g., a terminal).
                                .flatMap(Optional::stream)
                                .map(fieldSpec -> fieldSpec.name)
                                .collect(toSet());
                        newParserInfo = parserInfo.toPartialD(components);
                    }
                    newParserInfoMap.put(variable(newSelectionName + "_" + lastVar), newParserInfo);
                });

        // Create a selection rule for new derivations.
        var newSelection = new Selection(variable(newSelectionName), newDerivations.stream().map(Rule::lhs).toList());
        // This rule will share the parser info of the rule that will contain it (the start rule).
        // The start var must be a full parser:
        // - It cannot be a bridge because that doesn't make sense.
        // - It cannot be a partial parser because rules corresponding to partial parsers cannot contain a common prefix.
        final var startVarParserInfo = (ParserInfo.Full) astMetadata.requireParserInfo(startVar);
        // Take the last variable of any chain to determine the type of AST components corresponding to symbol in the common prefix.
        // Any chain can be used, as the prefix is common and so must be the AST component types.
        final var lastVarParserInfo = (ParserInfo.Full) astMetadata.requireParserInfo(first(commonPrefix.chains).getLast());
        newParserInfoMap.put(newSelection.lhs(),
                             startVarParserInfo.toPartialS(Stream.of(commonPrefix.symbol)
                                                                   .map(sym -> astMetadata.findAstNodeField(lastVarParserInfo.astNode(), sym))
                                                                   .flatMap(Optional::stream)
                                                                   .map(fs -> JavaPoet.asClassName(fs.type.box()))
                                                                   .collect(toSet())));

        // Create a new derivation rule that includes the common prefix.
        var newDerivationWithPrefixName = commonPrefix.chains.stream()
                .map(List::getLast)
                // Enable determinism of names
                .sorted()
                .collect(joining("_"));
        var newDerivationWithPrefix = new Derivation(variable(newDerivationWithPrefixName),
                                                     List.of(commonPrefix.symbol, newSelection.lhs()));
        // This rule will share the parser info of the rule that will contain it (the start rule).
        newParserInfoMap.put(newDerivationWithPrefix.lhs(), astMetadata.requireParserInfo(startVar));

        // The starting selection rule is modified:
        // * The new derivation with prefix rule is added to the RHS.
        // * Inline all variables contained in the middle of the chains (sublist from 1 to size-1) into the RHS.
        //   E.g., for chains A -> B -> F and A -> C, inline B: given B -> F | G, obtain A -> F_C | G
        // * All other parts of the RHS are preserved.
        var newStartVarRhs = append(inlineChains(commonPrefix, bnf), newDerivationWithPrefix.lhs());
        var newStartVarRule = new Selection(startVar, newStartVarRhs);

        var newBnf = bnf
                .addRules(append(newDerivations, newSelection, newDerivationWithPrefix))
                .updateRule(startVar, $ -> newStartVarRule);

        return new Data(newBnf, astMetadata.updateParserInfos(newParserInfoMap)).removeUnused();
    }

    private List<? extends Variable> inlineChains(
            final CommonPrefix commonPrefix,
            final BNF bnf)
    {
        Set<Variable> allVarsInChains = commonPrefix.chains.stream().flatMap(List::stream).collect(toSet());
        return allVarsInChains.stream()
                .flatMap(var -> bnf.requireRuleFor(var) instanceof Selection sel ? sel.rhs().stream() : Stream.empty())
                .distinct()
                .filter(var -> !allVarsInChains.contains(var))
                .filter(var -> !commonPrefix.symbol.equals(var))
                .toList();
    }

    private Optional<CommonPrefix> findFirstCommonPrefix(BNF bnf) {
        return bnf.rules().stream()
                // Sort to enable determinism.
                .sorted(Comparator.comparing(Rule::lhs))
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

        if (dx.lhs().equals(dy.lhs()))
            return Optional.of(dx.lhs());
        else if (Util.optionalMap2(firstOpt(dx.rhs()), firstOpt(dy.rhs()), Objects::equals).orElse(false))
            return firstOpt(dx.rhs());
        else return Optional.empty();
        // final var list = zip(xs.reversed(), ys.reversed())
        //         .takeWhile(pair -> pair.map((x, y) -> {
        //             if (bnf.requireRuleFor(x) instanceof Derivation dx && bnf.requireRuleFor(y) instanceof Derivation dy)
        //                 return !dx.rhs().isEmpty() && !dy.rhs().isEmpty() && dx.rhs()
        //                         .getFirst()
        //                         .equals(dy.rhs().getFirst());
        //             else return x.equals(y);
        //         }))
        //         .toList()
        //         .reversed();


        // List<Symbol> prefix = new ArrayList<>();
        // for (int i = 0; i < xs.size() && i < ys.size(); i++) {
        //     if (xs.get(i).equals(ys.get(i))) {
        //         prefix.add(xs.get(i));
        //     } else {
        //         return prefix.isEmpty() ? Optional.empty() : Optional.of(prefix);
        //     }
        // }
        // return prefix.isEmpty() ? Optional.empty() : Optional.of(prefix);
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
                                    commonPrefixes.stream().map(CommonPrefix::chains).flatMap(Collection::stream).toList());
        }

    }

    public record Data (BNF bnf, AstMetadata astMetadata) {
        public Data removeUnused() {
            return bnf.removeUnused()
                    .map((newBnf, unused) -> new Data(newBnf, astMetadata.removeVariables(unused)));
        }
    }

}
