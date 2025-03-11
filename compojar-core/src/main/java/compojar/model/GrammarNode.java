package compojar.model;

/**
 * A node in a grammar tree.
 * <p>
 * In terms of {@link Object#equals(Object)}, all nodes are distinct. I.e., a node is equal only to itself.
 */
public sealed interface GrammarNode {

    CharSequence name();

    GrammarNode copy();

    /**
     * A full node corresponds to a left-hand side symbol of a derivation rule.
     * A node of this type should have a single child.
     */
    record Full (CharSequence name)
        implements GrammarNode
    {
        @Override
        public Full copy() {
            return new Full(name);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
    }

    /**
     * A leaf node corresponds to a terminal symbol.
     */
    record Leaf (CharSequence name)
        implements GrammarNode
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

    /**
     * A free node corresponds to a left-hand side symbol of a selection rule.
     * A node of this type should have one or more children.
     */
    record Free (CharSequence name)
        implements GrammarNode
    {
        @Override
        public Free copy() {
            return new Free(name);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
    }

}
