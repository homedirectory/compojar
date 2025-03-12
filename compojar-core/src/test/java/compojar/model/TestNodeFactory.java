package compojar.model;

class TestNodeFactory implements NodeFactory {
    private long n = 0;

    @Override
    public GrammarNode.Free newFreeNode() {
        return new GrammarNode.Free("$" + ++n);
    }

    @Override
    public GrammarNode.Free newFreeNode(CharSequence nameHint) {
        return new GrammarNode.Free(nameHint + "$" + ++n);
    }

    @Override
    public GrammarNode.Full newNode() {
        return new GrammarNode.Full("$" + ++n);
    }

    @Override
    public GrammarNode.Full newNode(CharSequence nameHint) {
        return new GrammarNode.Full(nameHint + "$" + ++n);
    }

}
