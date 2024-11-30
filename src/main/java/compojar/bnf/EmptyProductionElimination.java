package compojar.bnf;

import compojar.gen.Namer;
import compojar.gen.ParserInfo;
import compojar.util.T2;
import compojar.util.T3;

import java.util.List;
import java.util.Optional;

import static compojar.bnf.Symbol.terminal;
import static compojar.util.T2.t2;
import static compojar.util.T3.t3;
import static compojar.util.Util.*;
import static java.lang.String.format;

/**
 * Grammar transformer that replaces some empty productions by using the technique of "implicitly parsed nodes".
 * This transformer can handle only a specific class of empty productions.
 * <p>
 * It is able to transform the following grammar:
 * <pre>
 * B -> D b
 * C -> D c
 * D -> d
 * </pre>
 * into
 * <pre>
 * O -> d _O
 * _O -> _B | _C
 * _B -> b # implicitly parses _D
 * _C -> c # implicitly parses _D
 * _D -> $
 * </pre>
 *
 * and the resulting API can be used to parse either B or C without using
 * the $() method of _D.
 * <p>
 * However, the following, inherentely ambiguous grammar cannot be
 * transformed with this technique:
 * <pre>
 * B -> D
 * C -> D
 * D -> d
 * </pre>
 * Rules B and C have equal RHS and it is impossible to know which one
 * should be parsed without introducing additional terminals into their RHS.
 * <p>
 * Also, the following grammar cannot be transformed with this technique:
 * <pre>
 * B -> D b
 * C -> E c
 * E -> D | F
 * F -> f
 * D -> d
 * </pre>
 *
 * The limiting factor here is the inability to handle selection rules that
 * derive a common prefix with another rule. Here that will be 'B -> D' and
 * 'C -> E -> D'. The latter chain creates challenges because only one
 * choice of 'E' leads to a common prefix.
 */
public class EmptyProductionElimination {

    private final Namer namer;
    private final Terminal specialEmptyTerminal;

    public EmptyProductionElimination(final Namer namer) {
        this.namer = namer;
        this.specialEmptyTerminal = terminal(namer.specialEmptyMethodName());
    }

    public Optional<T2<BNF, AstMetadata>> apply(BNF bnf, AstMetadata astMetadata) {
        return apply_(bnf, astMetadata)
                .map(pair -> pair.map(this::apply).orElse(pair));
    }

    private Optional<T2<BNF, AstMetadata>> apply_(BNF bnf, AstMetadata astMetadata) {
        // 1. Let r be a rule whose RHS is empty.
        // 2. For each rule u where r is the first RHS symbol, drop the first RHS symbol and mark u as implicitly parsing r.
        // 3. Replace RHS of r with $.
        var result = reduce(bnf.rules().stream(),
                            t3(bnf, astMetadata, 0),
                            (acc, rule) -> transformRule(rule, acc));
        return result.thd() > 0 ? Optional.of(t2(result.fst(), result.snd())) : Optional.empty();
    }

    /**
     * The triple is defined to be (bnf, ast metadata, number of transformed rules so far)
     */
    private T3<BNF, AstMetadata, Integer> transformRule(Rule r, T3<BNF, AstMetadata, Integer> result) {
        if (!r.rhs().isEmpty()) {
            return result;
        }
        else {
            return switch (r) {
                case Selection $ -> result;
                case Derivation rd -> {
                    var us = result.fst().rules().stream()
                            .filter(u -> switch (u) {
                                case Derivation d -> firstOpt(d.rhs()).filter(r.lhs()::equals).isPresent();
                                case Selection $ -> false;
                            })
                            .toList();
                    var newResult = reduce(
                            us.stream(),
                            result,
                            (acc, u) -> acc.map((bnf, astMetadata, count) -> {
                                return t3(bnf.overrideRule(((Derivation) u).updateRhs(rhs -> subList(rhs, 1))),
                                          astMetadata.updateParserInfo(u.lhs(), info -> switch (info) {
                                              case ParserInfo.Full full ->
                                                      full.setImplicitVar(r.lhs());
                                              case ParserInfo.PartialD d ->
                                                      d.setImplicitVar(r.lhs());
                                              default -> throw new IllegalStateException(format("Unexpected parser info: %s", info));
                                          }),
                                          count + 1);
                            }));
                    var newR = rd.updateRhs($ -> List.of(specialEmptyTerminal));
                    yield newResult.map1(bnf -> bnf.overrideRule(newR));
                }
            };
        }
    }

}
