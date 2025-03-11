package compojar.model;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static compojar.model.Keys.*;
import static compojar.util.T3.t3;
import static compojar.util.Util.foldl;
import static compojar.util.Util.zipWith;

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
                    -> inlineFreeNode(freeNode, newModel, nodeFactory);
            default -> throw new IllegalStateException();
        };
    }

    private static GrammarTreeModel inlineFreeNode(
            GrammarNode.Free node,
            GrammarTreeModel model,
            NodeFactory nodeFactory)
    {
        var parent = model.requireAttribute(node, PARENT);
        var maybeNext = model.get(node, NEXT);
        var children = model.get(node, CHILDREN).orElseGet(Set::of);
        var ms = children.stream().map($ -> nodeFactory.newNode(parent.name())).toList();
        var nexts = children.stream().map($ -> maybeNext.map(GrammarNode::copy)).flatMap(Optional::stream).toList();

        model = model
                .addNodes(ms)
                .addNodes(nexts);
        model = foldl((accModel, m) -> accModel.copyAttributes(parent, m), model, ms);
        {
            final var newModel_ = model;
            model = maybeNext.map(next -> foldl((accModel, newNext) -> accModel.copyAttributes(next, newNext), newModel_, nexts))
                    .orElse(model);
        }
        model = model.setAll(zipWith(children, ms, (c, m) -> t3(c, PARENT, m)));
        model = model.setAll(maybeNext.map(next -> zipWith(children, nexts, (c, n) -> t3(c, NEXT, n)))
                                           .orElseGet(Stream::empty));
        return model
                .removeNodes(parent, node)
                .removeNodes(maybeNext.stream());
    }

    private static GrammarTreeModel inlineChildren(GrammarNode node, GrammarTreeModel model, NodeFactory nodeFactory) {
        return foldl((accModel, c) -> inline(accModel, c, nodeFactory),
                     model,
                     model.get(node, CHILDREN).orElseGet(Set::of));
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private Inline() {}

}
