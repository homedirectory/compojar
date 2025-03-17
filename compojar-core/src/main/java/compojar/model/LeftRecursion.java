package compojar.model;

import compojar.util.CharSequenceEnum;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static compojar.model.Keys.*;
import static compojar.util.Util.*;
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
        var newModel = Inline.inline(model, A, nodeFactory);

        // 2
        var L = findLrecLeaves(model, A, eqNode);
        if (L.isEmpty())
            throw new IllegalStateException("L must not be empty.");

        // 3
        newModel = removeAllUseless(newModel, A, L);
        var Lu = L.stream().filter(newModel::contains).collect(toSet());

        // 4
        var model_AL = foldl(LeftRecursion::removeLeaf, newModel.subtree(A), Lu);

        // 5
        final Optional<GrammarTreeModel> maybeModel_AR;
        if (Lu.isEmpty()) {
            maybeModel_AR = Optional.empty();
        }
        else {
            var model_A = newModel.subtree(A);
            var ancestors = model_A.nodes().stream().filter(node -> isAncestorOfAny(model_A, node, Lu)).toList();
            var nexts = concatSet(ancestors, Lu).stream().flatMap(anc -> allNexts(model_A, anc)).toList();
            maybeModel_AR = Optional.of(disconnect(model_A.retainNodes(concatSet(ancestors, nexts, Lu)),
                                                   Lu)
                                                .copy());
        }

        // 6
        var A_L = nodeFactory.newFreeNode(NameHints.A_L);
        model_AL = model_AL.setRoot(A_L);

        return newModel
                .removeSubtreeBelow(A)
                // 6
                .include(model_AL)
                .set(A_L, PARENT, A)
                // 7
                .pipe(m -> maybeModel_AR.map(model_AR -> {
                    GrammarNode A_R = nodeFactory.newFreeNode(NameHints.A_R),
                            A_R_copy = A_R.copy(),
                            Eps = nodeFactory.newNode(NameHints.Eps),
                            A_R_ = nodeFactory.newNode(NameHints.A_R_),
                            A_R__ = nodeFactory.newFreeNode(NameHints.A_R__);
                    return m.include(model_AR.setRoot(A_R__))
                            .addNodes(A_R, A_R_copy, Eps, A_R_)
                            .set(Eps, PARENT, A_R)
                            .set(A_R_, PARENT, A_R)
                            .set(A_R__, PARENT, A_R_)
                            .set(A_R__, NEXT, A_R_copy)
                            .set(A_L, NEXT, A_R);
                }).orElse(m))
                // 7
                .replaceNode(A, new GrammarNode.Full(A.name()));
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
        var subRoot = Stream.concat(Stream.of(leaf), ancestors(model, leaf))
                .filter(anc -> model.getF(anc, PARENT) instanceof GrammarNode.Free)
                .findFirst()
                .orElseThrow();
        return model.pruneSubtreeWithNext(subRoot);
    }

    static GrammarTreeModel disconnect(GrammarTreeModel model, Iterable<? extends GrammarNode> nodes) {
        return foldl((acc, node) -> acc.get(node, NEXT)
                                       .map(next -> acc.set(next, PARENT, acc.getF(node, PARENT)))
                                       .orElse(acc)
                                       .removeNode(node),
                     model,
                     nodes);
    }

    static boolean isUseless(GrammarTreeModel model, GrammarNode root, GrammarNode leaf) {
        return cons(leaf, ancestors(model, leaf))
                .takeWhile(anc -> anc != root)
                .noneMatch(anc -> model.has(anc, NEXT));
    }

    static GrammarTreeModel removeAllUseless(GrammarTreeModel model, GrammarNode root, Set<GrammarNode> leaves) {
        var garbage = leaves.stream()
                            .filter(l -> isUseless(model, root, l))
                            // Also remove useless ancestors of useless leaves.
                            .flatMap(l -> Stream.concat(Stream.of(l),
                                                        ancestors(model, l)
                                                                .takeWhile(anc -> anc != root)
                                                                .takeWhile(anc -> model.getF(anc, CHILDREN).size() < 2)))
                            .collect(toSet());
        return model.removeNodes(garbage);
    }

}
