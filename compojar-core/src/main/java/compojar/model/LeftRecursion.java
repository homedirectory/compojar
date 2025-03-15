package compojar.model;

import compojar.util.CharSequenceEnum;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static compojar.model.Keys.*;
import static compojar.util.Util.concatSet;
import static compojar.util.Util.foldl;
import static java.util.stream.Collectors.toSet;

public class LeftRecursion {

    public enum NameHints implements CharSequenceEnum {
        A_L, A_R, A_R_, A_R__, Eps
    }

    public static GrammarTreeModel removeLeftRecursion(GrammarTreeModel model, NodeFactory nodeFactory, Eq<GrammarNode> eqNode) {
        return findLrecNode(model, eqNode)
                .map(A -> removeLeftRecursion(model, A, nodeFactory, eqNode))
                .orElse(model);
    }

    private static GrammarTreeModel removeLeftRecursion(
            GrammarTreeModel model,
            GrammarNode A,
            NodeFactory nodeFactory,
            Eq<GrammarNode> eqNode)
    {
        // 1
        var inlinedModel = Inline.inline(model, A, nodeFactory);

        // 2
        var L = findLrecLeaves(model, A, eqNode);
        if (L.isEmpty())
            throw new IllegalStateException("L must not be empty.");

        // 3
        var model_AL = foldl(LeftRecursion::removeLeaf, inlinedModel.subtree(A), L);

        // 4
        final GrammarTreeModel model_AR;
        {
            var model_A = inlinedModel.subtree(A);
            var ancestors = model_A.nodes().stream().filter(node -> isAncestorOfAny(model_A, node, L)).toList();
            var nexts = concatSet(ancestors, L).stream().flatMap(anc -> allNexts(model_A, anc)).toList();
            model_AR = disconnect(model_A.retainNodes(concatSet(ancestors, nexts, L)),
                                  L)
                    .copy();
        }

        // 5
        var A_L = nodeFactory.newFreeNode(NameHints.A_L);
        model_AL = model_AL.setRoot(A_L);

        // 6
        GrammarNode A_R = nodeFactory.newFreeNode(NameHints.A_R),
                A_R_copy = A_R.copy(),
                Eps = nodeFactory.newNode(NameHints.Eps),
                A_R_ = nodeFactory.newNode(NameHints.A_R_),
                A_R__ = nodeFactory.newFreeNode(NameHints.A_R__);

        return inlinedModel
                .removeSubtreeBelow(A)
                // 5
                .include(model_AL)
                .set(A_L, PARENT, A)
                // 6
                .include(model_AR.setRoot(A_R__))
                .addNodes(A_R, A_R_copy, Eps, A_R_)
                .set(Eps, PARENT, A_R)
                .set(A_R_, PARENT, A_R)
                .set(A_R__, PARENT, A_R_)
                .set(A_R__, NEXT, A_R_copy)
                .set(A_L, NEXT, A_R)
                // 7
                .replaceNode(A, new GrammarNode.Full(A.name()));
    }

    /**
     * Map {@code target} by replacing all nodes that occur in {@code model} with copies.
     */
    private static GrammarTreeModel replaceWithCopies(GrammarTreeModel target, GrammarTreeModel model) {
        return foldl((acc, node) -> target.replaceNode(node, node.copy()),
                     target,
                     model.nodes().stream().filter(node -> target.nodes().contains(node)));
    }

    static Optional<GrammarNode> findLrecNode(GrammarTreeModel model, Eq<GrammarNode> eqNode) {
        return bfs(model)
                .filter(node -> leaves(model.subtree(node)).anyMatch(leaf -> eqNode.areEqual(node, leaf)))
                .findAny();
    }

    static Stream<GrammarNode> bfs(GrammarTreeModel model) {
        return bfs(model, Set.of(model.root()));
    }

    private static Stream<GrammarNode> bfs(GrammarTreeModel model, Set<GrammarNode> nodes) {
        return nodes.isEmpty()
                ? Stream.of()
                : Stream.concat(nodes.stream(),
                                bfs(model,
                                    nodes.stream().flatMap(node -> model.get(node, CHILDREN).orElseGet(Set::of).stream()).collect(toSet())));
    }

    static Stream<GrammarNode> leaves(GrammarTreeModel model) {
        return model.nodes()
                .stream()
                .filter(node -> model.has(node, PARENT) && model.get(node, CHILDREN).orElseGet(Set::of).isEmpty());
    }

    static Set<GrammarNode> findLrecLeaves(GrammarTreeModel model, GrammarNode root, Eq<GrammarNode> eqNode) {
        return leaves(model.subtree(root)).filter(leaf -> eqNode.areEqual(root, leaf)).collect(toSet());
    }

    static GrammarTreeModel removeLeaf(GrammarTreeModel model, GrammarNode leaf) {
        var ancestor = ancestors(model, leaf)
                .filter(anc -> model.getF(anc, PARENT) instanceof GrammarNode.Free)
                .findFirst()
                .orElseThrow();
        return model.pruneSubtree(ancestor);
    }

    static GrammarTreeModel disconnect(GrammarTreeModel model, Iterable<? extends GrammarNode> nodes) {
        return foldl((acc, node) -> acc.get(node, NEXT)
                                       .map(next -> acc.set(next, PARENT, acc.getF(node, PARENT)))
                                       .orElse(acc)
                                       .removeNode(node),
                     model,
                     nodes);
    }

}
