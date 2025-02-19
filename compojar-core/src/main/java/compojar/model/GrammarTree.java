package compojar.model;

public sealed interface GrammarTree {

    CharSequence name();

    GrammarTree copy();

    record Node (CharSequence name)
        implements GrammarTree
    {
        @Override
        public Node copy() {
            return new Node(name);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    record Leaf (CharSequence name)
        implements GrammarTree
    {
        @Override
        public Leaf copy() {
            return new Leaf(name);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    record FreeNode (CharSequence name)
        implements GrammarTree
    {
        @Override
        public FreeNode copy() {
            return new FreeNode(name);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
    }

}

// public sealed interface GrammarTree {
//
//     record Node (GrammarTree child, Optional<GrammarTree> next)
//             implements GrammarTree {}
//
//     record Leaf (Optional<GrammarTree> next)
//             implements GrammarTree {}
//
//     record FreeNode (Set<GrammarTree> children, Optional<GrammarTree> next)
//             implements GrammarTree {}
//
//     default Collection<GrammarTree> flatten() {
//         class $ {
//             static Collection<GrammarTree> flatten(Set<GrammarTree> acc, Set<GrammarTree> queue) {
//                 if (queue.isEmpty()) {
//                     return acc;
//                 }
//                 else {
//                     return flatten(concatSet(acc, queue),
//                                    queue.stream()
//                                            .flatMap(node -> Stream.concat(node.children().stream(), node.next().stream()))
//                                            .collect(toSet()));
//                 }
//             }
//         }
//
//         return $.flatten(Set.of(), Set.of(this));
//     }
//
//     default Collection<GrammarTree> children() {
//         return switch (this) {
//             case FreeNode node -> node.children;
//             case Leaf leaf -> List.of();
//             case Node node -> List.of(node.child);
//         };
//     }
//
//     default Optional<GrammarTree> next() {
//         return switch (this) {
//             case FreeNode node -> node.next;
//             case Leaf leaf -> leaf.next;
//             case Node node -> node.next;
//         };
//     }
//
// }

// public sealed interface GrammarTree<T> {
//
//     static <T> GrammarTree<T> tree() {
//         class $ {
//             static final GrammarTree<?> ROOT = new Node<>(null, null, null, null);
//         }
//         return (GrammarTree<T>) $.ROOT;
//     }
//
//     static boolean isRoot(GrammarTree<?> tree) {
//         if (tree == tree()) return true;
//         else return switch (tree) {
//             case GrammarTree.FreeNode<?> t -> t.parent() == tree();
//             case GrammarTree.Leaf<?> t -> t.parent() == tree();
//             case GrammarTree.Node<?> t -> t.parent() == tree();
//         };
//     }
//
//     record Node<T> (T type, Node<T> parent, GrammarTree<T> child, Set<RTree<T>> siblings)
//             implements GrammarTree<T> {}
//
//     record Leaf<T> (T type, Node<T> parent, Set<RTree<T>> siblings)
//             implements GrammarTree<T> {}
//
//     record FreeNode<T> (T type, Node<T> parent, Set<GrammarTree<T>> children, Set<RTree<T>> siblings)
//             implements GrammarTree<T> {}
//
//     sealed interface RTree<T> {}
//
//     record RNode<T> (T type, GrammarTree<T> child, Set<RTree<T>> siblings)
//             implements RTree<T> {}
//
//     record RLeaf<T> (T type, Set<RTree<T>> siblings)
//             implements RTree<T> {}
//
//     record RFreeNode<T> (T type, Set<GrammarTree<T>> children, Set<RTree<T>> siblings)
//             implements RTree<T> {}
//
// }
