package compojar.model;

import java.util.Set;

import static compojar.model.Keys.*;
import static compojar.util.T3.t3;
import static compojar.util.Util.foldl;

/**
 * Inlining of grammar trees.
 * <p>
 * Laws:
 * <ul>
 *   <li> In an inlined tree only the root node may be free.
 * </ul>
 */
public final class Inline {

    /**
     * Inlines the tree at the root.
     */
    public static GrammarTreeModel inline(GrammarTreeModel model, NodeFactory nodeFactory) {
        return inline(model, model.root(), nodeFactory);
    }

    /**
     * Inlines the tree at the specified node, effectively inlining the sub-tree rooted at the specified node.
     */
    public static GrammarTreeModel inline(GrammarTreeModel model, GrammarNode node, NodeFactory nodeFactory) {
        model.assertContains(node);

        final var newModel = inlineChildren(node, model, nodeFactory);

        if (node.equals(newModel.root()))
            return newModel;

        return switch (node) {
            case GrammarNode.Leaf $ -> newModel;
            case GrammarNode.Full $ -> newModel;
            case GrammarNode.Free freeNode
                    when PARENT.get(newModel, freeNode).filter(GrammarNode.Free.class::isInstance).isPresent()
                    ->
            {
                var parent = newModel.requireAttribute(freeNode, PARENT);
                var children = newModel.get(freeNode, CHILDREN).orElseGet(Set::of);
                yield newModel
                        .removeNode(freeNode)
                        .setAll(children.stream().map(c -> t3(c, PARENT, parent)));
            }
            case GrammarNode.Free freeNode
                    when PARENT.get(newModel, freeNode).filter(GrammarNode.Full.class::isInstance).isPresent()
                    -> inlineFreeNodeWithFullParent(freeNode, newModel, nodeFactory);
            default -> throw new IllegalStateException();
        };
    }

    private static GrammarTreeModel inlineFreeNodeWithFullParent(
            GrammarNode.Free node,
            GrammarTreeModel model,
            NodeFactory nodeFactory)
    {
        var parent = model.requireAttribute(node, PARENT);

        return foldl((accModel, child) -> {
                         var p = nodeFactory.newNode(parent.name());
                         return accModel
                                 .addNode(p)
                                 .set(p, PARENT, parent)
                                 .maybeMap(model.get(parent, NEXT).map(GrammarNode::copy),
                                           (pNext, m) -> m.addNode(pNext)
                                                          .set(p, NEXT, pNext))
                                 .set(child, PARENT, p)
                                 .maybeMap(model.get(node, NEXT).map(GrammarNode::copy),
                                           (next, m) -> m.addNode(next)
                                                         .set(child, NEXT, next));
                     },
                     model,
                     model.get(node, CHILDREN).orElseGet(Set::of))
                .removeAttribute(NEXT, parent)
                .replaceNode(parent, new GrammarNode.Free(parent.name()))
                .removeNode(model.get(node, NEXT))
                .removeNode(model.get(parent, NEXT))
                .removeNode(node);
    }

    private static GrammarTreeModel inlineChildren(GrammarNode node, GrammarTreeModel model, NodeFactory nodeFactory) {
        return foldl((accModel, c) -> inline(accModel, c, nodeFactory),
                     model,
                     model.get(node, CHILDREN).orElseGet(Set::of));
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private Inline() {}

}
