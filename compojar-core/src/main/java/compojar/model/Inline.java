package compojar.model;

import java.util.List;
import java.util.Set;

import static compojar.model.Keys.*;
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
        return inline(model, model.root(), nodeFactory);
    }

    /**
     * Inlines the tree at the specified node, effectively inlining the sub-tree rooted at the specified node.
     */
    public static GrammarTreeModel inline(GrammarTreeModel model, GrammarNode node, NodeFactory nodeFactory) {
        model.assertContains(node);

        final var newModel = inlineChildren(node, model, nodeFactory);

        return model.get(node, PARENT)
                    .map(parent -> switch (node) {
                        case GrammarNode.Leaf $ -> newModel;
                        case GrammarNode.Full $ -> newModel;
                        case GrammarNode.Free freeNode when hasLinks(model, freeNode) -> newModel;
                        case GrammarNode.Free freeNode when parent instanceof GrammarNode.Free -> {
                            var children = newModel.get(freeNode, CHILDREN).orElseGet(Set::of);
                            yield newModel
                                    .removeNode(freeNode)
                                    .setAll(children.stream().map(c -> t3(c, PARENT, parent)));
                        }
                        case GrammarNode.Free freeNode when parent instanceof GrammarNode.Full ->
                                inlineFreeNodeWithFullParent(freeNode, newModel, nodeFactory);
                        default -> throw new IllegalStateException();
                    })
                    .orElse(newModel);
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
                                 .pipe(m -> foldl2((accM, accNexts, next) -> accM.addCopyOf(next, TARGET).map2(pNext -> cons(pNext, accNexts)),
                                                   m, List.<GrammarNode>of(),
                                                   allNexts(m, parent))
                                         .map((m_, pNexts) -> linkNext(m_, cons(p, pNexts.reversed()))))
                                 .set(child, PARENT, p)
                                 .pipe(m -> foldl2((accM, accNexts, next) -> accM.addCopyOf(next, TARGET).map2(newNext -> cons(newNext, accNexts)),
                                                   m, List.<GrammarNode>of(),
                                                   allNexts(m, node))
                                         .map((m_, newNexts) -> linkNext(m_, cons(child, newNexts.reversed()))));
                     },
                     model,
                     model.get(node, CHILDREN).orElseGet(Set::of))
                .removeAttribute(NEXT, parent)
                .replaceNode(parent, new GrammarNode.Free(parent.name()))
                .removeNodes(allNexts(model, node))
                .removeNodes(allNexts(model, parent))
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
