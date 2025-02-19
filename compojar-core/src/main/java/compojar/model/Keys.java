package compojar.model;

import compojar.model.GrammarTree.Leaf;
import compojar.util.Util;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static compojar.util.Util.remove;
import static java.lang.String.format;

public final class Keys {

    public static final Key<GrammarTree> PARENT = new NamedKey<>("parent") {
        @Override
        public GrammarTreeModel _set(GrammarTreeModel model, GrammarTree node, GrammarTree parent, Optional<GrammarTree> maybeOldParent) {
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
        public GrammarTreeModel _remove(GrammarTreeModel model, GrammarTree node, GrammarTree parent) {
            return model.updateAttribute(parent, CHILDREN, children -> remove(children, node));
        }

        @Override
        protected GrammarTreeModel removeNode(
                GrammarTreeModel model,
                GrammarTree node,
                GrammarTree parent,
                GrammarTree removedNode)
        {
            if (removedNode.equals(parent)) {
                return model.removeAttribute(PARENT, node);
            }
            else {
                return model;
            }
        }
    };

    public static Stream<GrammarTree> ancestors(GrammarTreeModel model, GrammarTree node) {
        return PARENT.get(model, node)
                .map(parent -> Stream.concat(Stream.of(parent), ancestors(model, parent)))
                .orElseGet(Stream::of);
    }

    public static boolean isAncestor(GrammarTreeModel model, GrammarTree ancestor, GrammarTree node) {
        return ancestors(model, node).anyMatch(ancestor::equals);
    }

    public static final Key<Set<GrammarTree>> CHILDREN = new NamedKey<>("children") {
        @Override
        public GrammarTreeModel _set(
                GrammarTreeModel model,
                GrammarTree node,
                Set<GrammarTree> children,
                Optional<Set<GrammarTree>> maybeOldChildren)
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
        public boolean canCopy(GrammarTreeModel model, GrammarTree from, GrammarTree to, Set<GrammarTree> attribute) {
            return false;
        }
    };

    public static Stream<GrammarTree> allChildren(GrammarTreeModel model, GrammarTree node) {
        return model.get(node, CHILDREN).orElseGet(Set::of)
                .stream()
                .flatMap(c -> Stream.concat(Stream.of(c), allChildren(model, c)));
    }

    public static final Key<GrammarTree> NEXT = new NamedKey<>("next") {
        @Override
        public GrammarTreeModel _set(
                GrammarTreeModel model,
                GrammarTree node,
                GrammarTree next,
                Optional<GrammarTree> maybeOldNext)
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
    };

    public static Stream<GrammarTree> allNexts(GrammarTreeModel model, GrammarTree node) {
        return model._getAttribute(node, NEXT)
                    .map(next -> Stream.concat(Stream.of(next), allNexts(model, next)))
                    .orElseGet(Stream::of);
    }


    private Keys() {}

}
