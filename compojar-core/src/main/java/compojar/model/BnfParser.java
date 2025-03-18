package compojar.model;

import compojar.bnf.*;
import compojar.util.T2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import static compojar.model.Keys.*;
import static compojar.util.T2.t2;
import static compojar.util.Util.*;

/**
 * Parse a BNF into a grammar tree.
 */
public final class BnfParser {

    public record Result (GrammarTreeModel model,
                          Map<GrammarNode, Symbol> nodeSymbolMap)
    {
        public GrammarNode nodeFor(Symbol symbol) {
            return findNode(symbol::equals);
        }

        public GrammarNode findNode(Predicate<? super Symbol> predicate) {
            return stream(nodeSymbolMap, (node, sym) -> predicate.test(sym) ? Optional.of(node) : Optional.<GrammarNode>empty())
                    .flatMap(Optional::stream)
                    .findFirst()
                    .orElseThrow();
        }

        public GrammarNode findNodeWithSymbol(BiPredicate<? super Symbol, ? super GrammarNode> predicate) {
            return stream(nodeSymbolMap, (node, sym) -> predicate.test(sym, node) ? Optional.of(node) : Optional.<GrammarNode>empty())
                    .flatMap(Optional::stream)
                    .findFirst()
                    .orElseThrow();
        }

        private Result addNode(GrammarNode node, Symbol symbol) {
            return new Result(model.addNode(node), insert(nodeSymbolMap, node, symbol));
        }

        private Result addNodes(List<? extends T2<? extends GrammarNode, ? extends Symbol>> pairs) {
            return foldl((acc, pair) -> pair.map(acc::addNode),
                         this,
                         pairs);
        }

        public Result mapModel(Function<? super GrammarTreeModel, GrammarTreeModel> fn) {
            return new Result(fn.apply(model), nodeSymbolMap);
        }
    }

    public static Result parseBnf(BNF bnf) {
        var startRule = bnf.requireRuleFor(bnf.start());
        var root = makeNode(startRule);
        var initModel = new GrammarTreeModel(Set.of(root), root);
        return parseRhs(bnf, startRule, root, new Result(initModel, Map.of(root, bnf.start())), Map.of(bnf.start(), root));
    }

    private static Result parseRhs(BNF bnf, Rule rule, GrammarNode lhsNode, Result result, Map<Symbol, GrammarNode> stack) {
        return switch (rule) {
            // Full node
            case Derivation derivation -> {
                if (derivation.rhs().isEmpty()) {
                    yield result;
                }
                else {
                    yield uncons(derivation.rhs())
                            .get()
                            .map((hd, tl) -> {
                                return parseSymbol(bnf, hd, result, stack)
                                        .map((result2, hdNode) -> {
                                            var result3 = result2.mapModel(model -> model.set(hdNode, PARENT, lhsNode));
                                            return foldl((acc, sym) -> acc.map((accResult, accTlNodes) -> {
                                                             return parseSymbol(bnf, sym, accResult, stack)
                                                                     .map((accResult_, symNode) -> t2(accResult_, cons(symNode, accTlNodes)));
                                                         }),
                                                         t2(result3, List.<GrammarNode>of()),
                                                         tl)
                                                    .map((result4, tlNodes) -> setNexts(cons(hdNode, tlNodes.reversed()), result4));
                                        });
                            });
                }
            }
            // Free node
            case Selection selection ->
                    foldl((result2, sym) -> parseSymbol(bnf, sym, result2, stack)
                                  .map((result3, symNode) -> result3.mapModel(model -> model.set(symNode, PARENT, lhsNode))),
                          result,
                          selection.rhs());
        };
    }

    private static Result setNexts(List<GrammarNode> nodes, Result result) {
        return foldl((accResult, nodePair) -> nodePair.map((a, b) -> accResult.mapModel(model -> model.set(a, NEXT, b))),
                     result,
                     zipWith(nodes, dropLeft(nodes, 1), T2::t2));
    }

    private static T2<Result, GrammarNode> parseSymbol(BNF bnf, Symbol symbol, Result result, Map<Symbol, GrammarNode> stack) {
        return switch (symbol) {
            case Terminal terminal -> {
                var node = new GrammarNode.Leaf(terminal.name());
                yield t2(result.addNode(node, terminal),
                         node);
            }
            case Variable variable -> {
                yield stream(stack, (stackSym, stackNode) -> Optional.of(stackNode).filter($ -> stackSym.normalEquals(variable)))
                        .flatMap(Optional::stream)
                        .findFirst()
                        .map(targetNode -> {
                            GrammarNode node = new GrammarNode.Leaf(variable.name());
                            return t2(result.addNode(node, variable).mapModel(m -> m.set(node, TARGET, targetNode)),
                                      node);
                        })
                        .orElseGet(() -> {
                            var rule = bnf.requireRuleFor(variable);
                            var node = makeNode(rule);
                            return t2(parseRhs(bnf, rule, node, result.addNode(node, variable), insert(stack, variable, node)),
                                      node);
                        });
            }
        };
    }

    private static GrammarNode makeNode(Rule rule) {
        return switch (rule) {
            case Derivation $ -> new GrammarNode.Full(rule.lhs().name());
            case Selection $ -> new GrammarNode.Free(rule.lhs().name());
        };
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private BnfParser() {}

}
