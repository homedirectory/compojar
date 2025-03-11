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
