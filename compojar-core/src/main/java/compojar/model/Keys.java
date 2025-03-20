package compojar.model;

import compojar.model.GrammarNode.Leaf;
import compojar.util.Util;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static compojar.model.Validation.Result.fail;
import static compojar.model.Validation.Result.ok;
import static compojar.util.Util.*;
import static java.lang.String.format;

public final class Keys {

    public static final Key<GrammarNode> PARENT = new NamedKey<>("parent") {
        @Override
        public GrammarTreeModel _set(GrammarTreeModel model, GrammarNode node, GrammarNode parent, Optional<GrammarNode> maybeOldParent) {
            model.assertContainsAll(node, parent);

            if (node.equals(parent)) {
                throw new IllegalArgumentException(format("Node cannot be its own parent.\nNode: [%s]", node));
            }
            if (parent instanceof Leaf leaf) {
                throw new IllegalArgumentException(format("Leaf cannot be a parent.\nParent: [%s]\nNode: [%s] ", leaf, node));
            }
            else if (model._getAttribute(parent, PARENT).filter(node::equals).isPresent()) {
                // TODO Handle indirect cycles
                throw new IllegalArgumentException(format("Circular parent relationship!\nNode 1: [%s]\nNode 2: [%s]",
                                                          node, parent));
            }
            else {
                final var result = super._set(model, node, parent, maybeOldParent)
                                        .setAttributeWith(parent, CHILDREN, Set.of(node), Util::concatSet);
                return maybeOldParent
                        .map(oldParent -> result.updateAttribute(oldParent, CHILDREN, children -> remove(children, node)))
                        .orElse(result);
            }
        }

        @Override
        public GrammarTreeModel _remove(GrammarTreeModel model, GrammarNode node, GrammarNode parent) {
            return model.updateAttribute(parent, CHILDREN, children -> remove(children, node));
        }

        @Override
        protected GrammarTreeModel removeNode(
                GrammarTreeModel model,
                GrammarNode node,
                GrammarNode parent,
                GrammarNode removedNode)
        {
            if (removedNode.equals(parent)) {
                return model.removeAttribute(PARENT, node);
            }
            else {
                return model;
            }
        }

        @Override
        protected GrammarTreeModel replaceNode(
                GrammarTreeModel model,
                GrammarNode node,
                GrammarNode parent,
                GrammarNode oldNode,
                GrammarNode newNode)
        {
            if (oldNode.equals(parent)) {
                return super._set(model, node, newNode, Optional.of(parent));
            }
            else {
                return super.replaceNode(model, node, parent, oldNode, newNode);
            }
        }

        @Override
        protected Validation.Result validate(GrammarTreeModel model, GrammarNode node, GrammarNode parent) {
            return ok()
                    .combine(node.equals(model.root())
                            ? fail("Root node cannot have a parent. Root: [%s], parent: [%s].",
                                   node, parent)
                            : ok())
                    .combine(parent instanceof Leaf
                                     ? fail("Leaf node cannot be a parent node. Parent: [%s], child: [%s].",
                                            parent, node)
                                     : ok());
        }
    };

    public static Stream<GrammarNode> ancestors(GrammarTreeModel model, GrammarNode node) {
        return PARENT.get(model, node)
                .map(parent -> Stream.concat(Stream.of(parent), ancestors(model, parent)))
                .orElseGet(Stream::of);
    }

    public static boolean isAncestor(GrammarTreeModel model, GrammarNode ancestor, GrammarNode node) {
        return ancestors(model, node).anyMatch(ancestor::equals);
    }

    public static boolean isAncestorOfAny(
            GrammarTreeModel model,
            GrammarNode ancestor,
            Iterable<? extends GrammarNode> nodes)
    {
        return stream(nodes).anyMatch(node -> isAncestor(model, ancestor, node));
    }

    public static final Key<Set<GrammarNode>> CHILDREN = new NamedKey<>("children") {
        @Override
        public GrammarTreeModel _set(
                GrammarTreeModel model,
                GrammarNode node,
                Set<GrammarNode> children,
                Optional<Set<GrammarNode>> maybeOldChildren)
        {
            if (children.contains(node)) {
                throw new IllegalArgumentException(format("Node cannot be its own child.\nNode: [%s]\nChildren: [%s]",
                                                          node, children));
            }
            else {
                return super._set(model, node, children, maybeOldChildren);
            }
        }

        @Override
        public boolean canCopy(GrammarTreeModel model, GrammarNode from, GrammarNode to, Set<GrammarNode> attribute) {
            return false;
        }

        @Override
        protected GrammarTreeModel replaceNode(
                GrammarTreeModel model,
                GrammarNode node,
                Set<GrammarNode> children,
                GrammarNode oldNode,
                GrammarNode newNode)
        {
            return super._set(model, node, replace(children, oldNode, newNode), Optional.of(children));
        }

        @Override
        protected Validation.Result validate(GrammarTreeModel model, GrammarNode node, Set<GrammarNode> children)
        {
            return ok()
                    .combine(node instanceof GrammarNode.Free && children.isEmpty()
                                     ? fail("Free node [%s] cannot have empty children.", node)
                                     : ok())
                    .combine(node instanceof GrammarNode.Full && children.size() != 1
                            ? fail("Full node [%s] must have exactly 1 child, but had %s: %s",
                                   node, children.size(), children)
                            : ok())
                    .combine(node instanceof Leaf && !children.isEmpty()
                            ? fail("Leaf node [%s] cannot have children.", node)
                            : ok());
        }
    };

    public static Stream<GrammarNode> allChildren(GrammarTreeModel model, GrammarNode node) {
        return model.get(node, CHILDREN).orElseGet(Set::of)
                .stream()
                .flatMap(c -> Stream.concat(Stream.of(c), allChildren(model, c)));
    }

    public static final Key<GrammarNode> NEXT = new NamedKey<>("next") {
        @Override
        public GrammarTreeModel _set(
                GrammarTreeModel model,
                GrammarNode node,
                GrammarNode next,
                Optional<GrammarNode> maybeOldNext)
        {
            model.assertContains(node);
            model.assertContains(next);

            if (node.equals(next)) {
                throw new IllegalArgumentException(format("Node cannot be its own next.\nNode: [%s]", node));
            }
            else {
                return super._set(model, node, next, maybeOldNext);
            }
        }

        @Override
        protected GrammarTreeModel replaceNode(
                GrammarTreeModel model,
                GrammarNode node,
                GrammarNode next,
                GrammarNode oldNode,
                GrammarNode newNode)
        {
            if (oldNode.equals(next)) {
                return super._set(model, node, newNode, Optional.of(next));
            }
            else {
                return super.replaceNode(model, node, next, oldNode, newNode);
            }
        }

        @Override
        protected Validation.Result validate(GrammarTreeModel model, GrammarNode node, GrammarNode next) {
            return ok()
                    .combine(node.equals(model.root())
                            ? fail("Root node cannot have a next node. Root: [%s], next: [%s].",
                                   node, next)
                            : ok())
                    .combine(model.has(next, PARENT)
                                     ? fail("Next node cannot have a parent node. This: [%s], next: [%s], parent: [%s].",
                                            node, next, model.getF(next, PARENT))
                                     : ok())
                    .combine(() -> {
                        var allWithThisNext = model
                                .nodes()
                                .stream()
                                .filter(nd -> model.get(nd, NEXT).filter(next::equals).isPresent())
                                .toList();
                        return List.of(node).equals(allWithThisNext)
                                ? ok()
                                : fail("Next node cannot be shared with other nodes. Next node [%s] is shared by [%s]",
                                       next, Util.join(allWithThisNext, ", "));
                    });
        }
    };

    public static Stream<GrammarNode> allNexts(GrammarTreeModel model, GrammarNode node) {
        return model._getAttribute(node, NEXT)
                    .map(next -> Stream.concat(Stream.of(next), allNexts(model, next)))
                    .orElseGet(Stream::of);
    }

    public static GrammarTreeModel linkNext(GrammarTreeModel model, List<? extends GrammarNode> nodes) {
        return foldl((acc, pair) -> pair.map((a, b) -> acc.set(a, NEXT, b)),
                     model,
                     zip(nodes, dropLeft(nodes, 1)));
    }

    /**
     * Represents a link from a node to a target node.
     * This relationship is used to represent recursive grammar rules (any recursion, not just left).
     * It ensures that grammar trees have finite size.
     * <p>
     * The linking node is always a leaf.
     */
    public static final Key<GrammarNode> TARGET = new NamedKey<>("target") {
        @Override
        protected GrammarTreeModel _set(
                GrammarTreeModel model,
                GrammarNode node,
                GrammarNode target,
                Optional<GrammarNode> maybeOldValue)
        {
            model.assertContains(node);
            model.assertContains(target);

            if (node.equals(target)) {
                throw new IllegalArgumentException(format("Node cannot be its own target.\nNode: [%s]", node));
            }

            if (!(node instanceof Leaf)) {
                throw new IllegalArgumentException(format("Linking node [%s] must be a leaf but was [%s].",
                                                          node, node.getClass().getSimpleName()));
            }

            if (target instanceof Leaf) {
                throw new IllegalArgumentException(format("Target node [%s] cannot be a leaf.", node));
            }

            return super._set(model, node, target, maybeOldValue);
        }

        @Override
        protected GrammarTreeModel removeNode(
                GrammarTreeModel model,
                GrammarNode node,
                GrammarNode target,
                GrammarNode removedNode)
        {
            if (removedNode.equals(target)) {
                throw new IllegalStateException(format("Node [%s] cannot be removed while being used as a target by [%s].",
                                                       target, node));
            }
            else {
                return super.removeNode(model, node, target, removedNode);
            }
        }

        @Override
        protected GrammarTreeModel replaceNode(
                GrammarTreeModel model,
                GrammarNode node,
                GrammarNode target,
                GrammarNode oldNode,
                GrammarNode newNode)
        {
            if (oldNode.equals(target)) {
                return super._set(model, node, newNode, Optional.of(target));
            }
            else {
                return super.replaceNode(model, node, target, oldNode, newNode);
            }
        }

        @Override
        protected Validation.Result validate(GrammarTreeModel model, GrammarNode node, GrammarNode target) {
            return ok()
                    .combine(node.equals(model.root())
                                     ? fail("Root node cannot have a target node. Root: [%s], target: [%s].",
                                            node, target)
                                     : ok())
                    .combine(node instanceof Leaf
                                     ? ok()
                                     : fail("Link node [%s] with target [%s] must be a leaf node.",
                                            node, target))
                    .combine(target instanceof Leaf
                                     ? fail("Target node [%s] linked by [%s] cannot be a leaf.")
                                     : ok());
        }
    };

    public static boolean hasLinks(GrammarTreeModel model, GrammarNode node) {
        return model.nodes().stream().anyMatch(nd -> model.get(nd, TARGET).filter(node::equals).isPresent());
    }

    public static Stream<GrammarNode> linksTo(GrammarTreeModel model, GrammarNode node) {
        return model.nodes().stream().filter(nd -> model.get(nd, TARGET).filter(node::equals).isPresent());
    }

    private Keys() {}

}
