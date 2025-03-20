package compojar.model;

import compojar.bnf.AbstractGrammar;
import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;
import compojar.model.GrammarNode.Free;
import compojar.model.GrammarNode.Full;
import org.junit.Test;

import static compojar.bnf.BnfBuilder.start;
import static compojar.model.Eq.eqOn;
import static compojar.model.TestUtils.structEquals;
import static org.junit.Assert.assertTrue;

public class InlineTest {

    @Test
    public void inline_node_whose_parent_is_free_node() {
        var inBnf = new AbstractGrammar() {
            Variable E, E1, X, Y;
            Terminal x, y;

            BNF bnf() {
                return start(E)
                        .select(E, E1, Y)
                        .select(E1, X)
                        .derive(X, x)
                        .derive(Y, y)
                        .$();
            }
        }.bnf();

        var inModel = BnfParser.parseBnf(inBnf).model();

        var expectedBnf = new AbstractGrammar() {
            Variable E, X, Y;
            Terminal x, y;

            BNF bnf() {
                return start(E)
                        .select(E, X, Y)
                        .derive(X, x)
                        .derive(Y, y)
                        .$();
            }
        }.bnf();

        var expectedModel = BnfParser.parseBnf(expectedBnf).model();

        var inlinedModel = Inline.inline(inModel, new TestNodeFactory());

        assertTrue(structEquals(expectedModel, inlinedModel, eqOn(GrammarNode::name)));
    }

    @Test
    public void inline_node_whose_parent_is_regular_node_01() {
        var inGrammar = new AbstractGrammar() {
            Variable E, Var, E1, E2, Add, Neg;
            Terminal one, two, plus, x, minus;

            BNF bnf() {
                return start(E)
                        .select(E, Var, E1)
                        .derive(Var, x)
                        .derive(E1, E2, one, two)
                        .select(E2, Add, Neg)
                        .derive(Add, plus)
                        .derive(Neg, minus)
                        .$();
            }
        };

        var expectedGrammar = new AbstractGrammar() {
            Variable E, Var, E1$1, E1$2, Add, Neg;
            Terminal one, two, plus, x, minus;

            BNF bnf() {
                return start(E)
                        .select(E, Var, E1$1, E1$2)
                        .derive(Var, x)
                        .derive(E1$1, Add, one, two)
                        .derive(E1$2, Neg, one, two)
                        .derive(Add, plus)
                        .derive(Neg, minus)
                        .$();
            }
        };

        var inModel = BnfParser.parseBnf(inGrammar.bnf()).model();
        var expectedModel = BnfParser.parseBnf(expectedGrammar.bnf()).model();

        var nodeFactory = new TestNodeFactory() {
            int n = 0;

            @Override
            public Full newNode(CharSequence nameHint) {
                return new Full(nameHint + "$" + (++n));
            }

            @Override
            public Free newFreeNode(CharSequence nameHint) {
                return new Free(nameHint + "$" + (++n));
            }
        };

        var inlinedModel = Inline.inline(inModel, nodeFactory);

        assertTrue(structEquals(expectedModel, inlinedModel, eqOn(GrammarNode::name)));
    }

    @Test
    public void inline_node_whose_parent_is_regular_node_02() {
        var inGrammar = new AbstractGrammar() {
            Variable E, Var, E0, E1, E2, Add, Neg;
            Terminal one, two, plus, x, minus, end;

            BNF bnf() {
                return start(E)
                        .select(E, Var, E0)
                        .derive(Var, x)
                        .derive(E0, E1, end)
                        .derive(E1, E2, one, two)
                        .select(E2, Add, Neg)
                        .derive(Add, plus)
                        .derive(Neg, minus)
                        .$();
            }
        };

        var expectedGrammar = new AbstractGrammar() {
            Variable E, Var, E0$3, E0$4, E1, E1$1, E1$2, Add, Neg;
            Terminal one, two, plus, x, minus, end;

            BNF bnf() {
                return start(E)
                        .select(E, Var, E0$3, E0$4)
                        .derive(Var, x)
                        .derive(E0$3, E1$1, end)
                        .derive(E0$4, E1$2, end)
                        .derive(E1$1, Add, one, two)
                        .derive(E1$2, Neg, one, two)
                        .derive(Add, plus)
                        .derive(Neg, minus)
                        .$();
            }
        };

        var inModel = BnfParser.parseBnf(inGrammar.bnf()).model();
        var expectedModel = BnfParser.parseBnf(expectedGrammar.bnf()).model();

        var nodeFactory = new TestNodeFactory() {
            int n = 0;

            @Override
            public Full newNode(CharSequence nameHint) {
                return new Full(nameHint + "$" + (++n));
            }

            @Override
            public Free newFreeNode(CharSequence nameHint) {
                return new Free(nameHint + "$" + (++n));
            }
        };

        var inlinedModel = Inline.inline(inModel, nodeFactory);

        assertTrue(structEquals(expectedModel, inlinedModel, eqOn(GrammarNode::name)));
    }

    @Test
    public void inline_recursively() {
        var inGrammar = new AbstractGrammar() {
            Variable E, E1, E2, E3, Sub, Var, Sub1, E2b, E2a, E2a1, E2a2;
            Terminal sqrt, x, sub1, e2b, e2a1, e2a2;

            BNF bnf() {
                return start(E)
                        .select(E, E1, E2)
                        .derive(E1, E3, sqrt)
                        .select(E3, Sub, Var)
                        .derive(Var, x)
                        .select(Sub, Sub1)
                        .derive(Sub1, sub1)
                        .select(E2, E2b, E2a)
                        .derive(E2b, e2b)
                        .select(E2a, E2a1, E2a2)
                        .derive(E2a1, e2a1)
                        .derive(E2a2, e2a2)
                        .$();
            };
        };

        var expectedGrammar = new AbstractGrammar() {
            Variable E, E1$1, E1$2, Var, E2b, E2a1, E2a2, Sub1;
            Terminal sqrt, x, sub1, e2b, e2a1, e2a2;

            BNF bnf() {
                return start(E)
                        .select(E, E1$1, E1$2, E2a1, E2a2, E2b)
                        .derive(E2b, e2b)
                        .derive(E1$1, Var, sqrt)
                        .derive(E1$2, Sub1, sqrt)
                        .derive(Var, x)
                        .derive(Sub1, sub1)
                        .derive(E2a1, e2a1)
                        .derive(E2a2, e2a2)
                        .$();
            };
        };

        var inModel = BnfParser.parseBnf(inGrammar.bnf()).model();
        var expectedModel = BnfParser.parseBnf(expectedGrammar.bnf()).model();

        var nodeFactory = new TestNodeFactory() {
            int n = 0;

            @Override
            public Full newNode(CharSequence nameHint) {
                return new Full(nameHint + "$" + (++n));
            }

            @Override
            public Free newFreeNode(CharSequence nameHint) {
                return new Free(nameHint + "$" + (++n));
            }
        };

        var inlinedModel = Inline.inline(inModel, nodeFactory);

        assertTrue(structEquals(expectedModel, inlinedModel, eqOn(GrammarNode::name)));
    }

    @Test
    public void recursive_nodes_are_not_inlined() {
        var g = new AbstractGrammar() {
            Variable S, E, N, Add;
            Terminal one, plus;

            BNF bnf() {
                return start(S)
                        .select(S, E)
                        .select(E, N, Add)
                        .derive(N, one)
                        .derive(Add, E, plus, E)
                        .$();
            }
        };

        var inModel = BnfParser.parseBnf(g.bnf()).model();

        var inlinedModel = Inline.inline(inModel, new TestNodeFactory());

        assertTrue(structEquals(inModel, inlinedModel, eqOn(GrammarNode::name)));
    }

    @Test
    public void inline_free_node_with_previous_and_next_nodes() {
        var g = new AbstractGrammar() {
            Variable S, E, Var, Add;
            Terminal begin, end, plus, x;

            BNF bnf() {
                return start(S)
                        .derive(S, begin, E, end)
                        .select(E, Var, Add)
                        .derive(Var, x)
                        .derive(Add, Var, plus, Var)
                        .$();
            }
        };

        var expectedG = new AbstractGrammar() {
            Variable S, S$1, S$2, Var, Add;
            Terminal begin, end, plus, x;

            BNF bnf() {
                return start(S)
                        .select(S, S$1, S$2)
                        .derive(S$1, begin, Add, end)
                        .derive(S$2, begin, Var, end)
                        .derive(Var, x)
                        .derive(Add, Var, plus, Var)
                        .$();
            }
        };

        var result = BnfParser.parseBnf(g.bnf());
        var inModel = result.model();

        var expectedModel = BnfParser.parseBnf(expectedG.bnf()).model();

        var inlinedModel = Inline.inline(inModel, new TestNodeFactory());

        assertTrue(structEquals(expectedModel, inlinedModel, eqOn(GrammarNode::name)));
    }

}
