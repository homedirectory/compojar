package compojar.model;

import compojar.bnf.*;
import compojar.util.T2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static compojar.model.Keys.NEXT;
import static compojar.model.Keys.PARENT;
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
        return parseRhs(bnf, startRule, root, new Result(initModel, Map.of(root, bnf.start())));
    }

    private static Result parseRhs(BNF bnf, Rule rule, GrammarNode lhsNode, Result result) {
        return switch (rule) {
            // Full node
            case Derivation derivation ->
                    uncons(derivation.rhs())
                            .get()
                            .map((hd, tl) -> {
                                var tlNodes = tl.stream().map(sym -> t2(new GrammarNode.Leaf(sym.name()), sym)).toList();
                                return parseSymbol(bnf, hd, result)
                                        .map((result2, hdNode) -> {
                                            var result3 = result2.addNodes(tlNodes)
                                                                 .mapModel(model -> model.set(hdNode, PARENT, lhsNode));
                                            return setNexts(cons(hdNode, tlNodes.stream().map(T2::fst).toList()), result3);
                                        });
                            });
            // Free node
            case Selection selection ->
                    foldl((result2, sym) -> parseSymbol(bnf, sym, result2)
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

    private static T2<Result, GrammarNode> parseSymbol(BNF bnf, Symbol symbol, Result result) {
        return switch (symbol) {
            case Terminal terminal -> {
                var node = new GrammarNode.Leaf(terminal.name());
                yield t2(result.addNode(node, terminal), node);
            }
            case Variable variable -> {
                var rule = bnf.requireRuleFor(variable);
                if (containsValue(result.nodeSymbolMap, variable)) {
                    // This is left recursion.
                    // If the node-symbol map contains this variable, then its rule had already been parsed.
                    var node = new GrammarNode.Leaf(variable.name());
                    yield t2(result.addNode(node, variable), node);
                }
                else {
                    var node = makeNode(rule);
                    yield t2(parseRhs(bnf, rule, node, result.addNode(node, variable)), node);
                }
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
