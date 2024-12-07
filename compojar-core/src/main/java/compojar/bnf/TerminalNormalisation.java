package compojar.bnf;

import compojar.gen.Namer;
import compojar.gen.ParserInfo;
import compojar.util.T2;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static compojar.bnf.Rule.derivation;
import static compojar.bnf.Symbol.variable;
import static compojar.util.T2.t2;
import static compojar.util.Util.reduce;
import static compojar.util.Util.replace;

public class TerminalNormalisation {

    private final Namer namer;

    public TerminalNormalisation(final Namer namer) {
        this.namer = namer;
    }

    public T2<BNF, AstMetadata> apply(final BNF bnf, final AstMetadata astMetadata) {
        var normalisedTerminals = new HashSet<Terminal>();
        var newBnf = reduce(bnf.rules().stream(),
                            bnf,
                            (acc, rule) -> normalise(rule)
                                    .map(pair -> acc.overrideRule(pair.peek2(normalisedTerminals::addAll).fst()))
                                    .orElse(acc))
                .addRules(normalisedTerminals.stream()
                                  .map(t -> derivation(variable(namer.normalisedTerminalName(t)), t))
                                  .toList());
        var newAstMetadata = reduce(normalisedTerminals.stream(),
                                    astMetadata,
                                    (acc, t) -> acc.addParserInfo(variable(namer.normalisedTerminalName(t)), new ParserInfo.Bridge(t)));
        return t2(newBnf, newAstMetadata);
    }

    private Optional<T2<Derivation, Set<Terminal>>> normalise(final Rule rule) {
        if (rule instanceof Derivation derivation) {
            Set<T2<Terminal, Integer>> terminals = new HashSet<>();

            for (int i = 1; i < derivation.rhs().size(); i++) {
                if (derivation.rhs().get(i) instanceof Terminal terminal) {
                    terminals.add(t2(terminal, i));
                }
            }

            if (terminals.isEmpty()) {
                return Optional.empty();
            }
            else {
                var newRule = reduce(terminals.stream(),
                                     derivation,
                                     (acc, pair) -> new Derivation(derivation.lhs(), replace(acc.rhs(), pair.snd(), variable(namer.normalisedTerminalName(pair.fst())))));
                return Optional.of(t2(newRule, terminals.stream().map(T2::fst).collect(Collectors.toSet())));
            }
        }
        else {
            return Optional.empty();
        }
    }

}
