package compojar.model;

import compojar.util.T2;
import compojar.util.Util;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static compojar.model.Keys.*;
import static compojar.util.T2.t2;
import static compojar.util.T3.t3;
import static compojar.util.Util.*;

/**
 * Inlining of grammar trees.
 * <p>
 * The following nodes cannot be inlined:
 * <ul>
 *   <li> Tree root.
 *   <li> Recursive node (any recursion, not just left).
 * </ul>
 */
public final class Inline {

    /**
     * Inlines the tree at the root.
     */
    public static GrammarTreeModel inline(GrammarTreeModel model, NodeFactory nodeFactory) {
        return fixpoint(model,
                        m -> foldl((accM, node) -> accM.contains(node) ? inlineNode(accM, node, nodeFactory) : accM,
                                   m,
                                   m.nodes()));
    }

    /**
     * Inlines the subtree at the specified node.
     */
    public static GrammarTreeModel inline(GrammarTreeModel model, GrammarNode node, NodeFactory nodeFactory) {
        if (node.equals(model.root())) {
            return inline(model, nodeFactory);
        }
        else {
            var inlinedSubtree = inline(model.subtree(node), nodeFactory);
            return model.replaceSubtree(node, inlinedSubtree);
        }
    }

    /**
     * Inlines only the specified node.
     */
    private static GrammarTreeModel inlineNode(GrammarTreeModel model, GrammarNode node, NodeFactory nodeFactory) {
        model.assertContains(node);

        return closestParent(model, node)
                .map(parent -> switch (node) {
                    case GrammarNode.Leaf $ -> model;
                    case GrammarNode.Full $ -> model;
                    case GrammarNode.Free freeNode when hasLinks(model, freeNode) -> model;
                    case GrammarNode.Free freeNode when parent instanceof GrammarNode.Free -> {
                        var children = model.get(freeNode, CHILDREN).orElseGet(Set::of);
                        yield model.removeNode(freeNode)
                                   .setAll(children.stream().map(c -> t3(c, PARENT, parent)));
                    }
                    case GrammarNode.Free freeNode when parent instanceof GrammarNode.Full ->
                            inlineFreeNodeWithFullParent(freeNode, parent, model, nodeFactory);
                    default -> throw new IllegalStateException();
                })
                .orElse(model);
    }

    /**
     * @param parent  closest parent, not necessarily the direct parent
     */
    private static GrammarTreeModel inlineFreeNodeWithFullParent(
            GrammarNode.Free node,
            GrammarNode parent,
            GrammarTreeModel model,
            NodeFactory nodeFactory)
    {
        var prevAndNextLists = prevAndNextLists(model, node);

        var newParent = new GrammarNode.Free(parent.name());
        var newModel = foldl(
                (accModel, child) -> {
                    var p = nodeFactory.newNode(parent.name());
                    return prevAndNextLists.map((prevs, nexts) -> {
                        return accModel.addCopiesOf(prevs, TARGET).map((accModel2, prevCopies) -> {
                            return accModel2.addCopiesOf(nexts, TARGET).map((accModel3, nextCopies) -> {
                                var body = concatList(prevCopies, List.of(child), nextCopies);
                                return accModel3
                                        .addNode(p)
                                        .set(p, PARENT, parent)
                                        .pipe(m -> setChildAndNexts(m, p, body));
                            });
                        });
                    });
                },
                model,
                model.get(node, CHILDREN).orElseGet(Set::of))
                .replaceNode(parent, newParent)
                // IDEA complains about the type here...
                .removeNodes((Collection<GrammarNode>) prevAndNextLists.map(Util::concatList))
                .removeNode(node);
        return newModel;
    }

    private static GrammarTreeModel setChildAndNexts(
            GrammarTreeModel model,
            GrammarNode parent,
            List<GrammarNode> body)
    {
        if (body.isEmpty()) {
            throw new IllegalArgumentException("Expected non-empty sequence.");
        }

        return model
                .set(body.getFirst(), PARENT, parent)
                .pipe(m -> linkNext(m, body));
    }

    private static T2<List<GrammarNode>, List<GrammarNode>> prevAndNextLists(GrammarTreeModel model, GrammarNode node) {
        return t2(allPrevious(model, node), allNexts(model, node).toList());
    }

    private static Optional<GrammarNode> closestParent(GrammarTreeModel model, GrammarNode node) {
        if (node.equals(model.root())) {
            return Optional.empty();
        }

        return Optional.of(model.get(node, PARENT)
                                .orElseGet(() -> {
                                    return previousNode(model, node)
                                            .flatMap(prev -> closestParent(model, prev))
                                            .orElseThrow();
                                }));
    }

    private static Optional<GrammarNode> previousNode(GrammarTreeModel model, GrammarNode node) {
        return model.nodes()
                    .stream()
                    .filter(nd -> model.get(nd, NEXT).filter(node::equals).isPresent())
                    .findFirst();
    }

    private static List<GrammarNode> allPrevious(GrammarTreeModel model, GrammarNode node) {
        return allPrevious(model, node, List.of());
    }

    private static List<GrammarNode> allPrevious(GrammarTreeModel model, GrammarNode node, List<GrammarNode> acc) {
        return previousNode(model, node)
                .map(prev -> allPrevious(model, prev, cons(prev, acc)))
                .orElse(acc);
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private Inline() {}

}
