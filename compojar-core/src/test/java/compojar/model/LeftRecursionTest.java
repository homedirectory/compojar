package compojar.model;

import compojar.bnf.AbstractGrammar;
import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;
import org.junit.Test;

import static compojar.bnf.BnfBuilder.start;
import static compojar.model.Eq.eqOn;
import static compojar.model.LeftRecursion.removeLeftRecursion;
import static compojar.model.TestUtils.structEquals;
import static org.junit.Assert.assertTrue;

public class LeftRecursionTest {

    @Test
    public void _01() {
        var g = new AbstractGrammar() {
            Variable E, N, Add;
            Terminal one, plus;

            BNF bnf() {
                return start(E)
                        .select(E, N, Add)
                        .derive(N, one)
                        .derive(Add, E, plus, E)
                        .$();
            }
        };

        var expectedG = new AbstractGrammar() {
            Variable E, $A_L, $A_R, $A_R_, $A_R__, $Eps, N, Add;
            Terminal one, plus;

            BNF bnf() {
                return start(E)
                        .derive(E, $A_L, $A_R)
                        .select($A_L, N)
                        .derive(N, one)
                        .select($A_R, $A_R_, $Eps)
                        .derive($A_R_, $A_R__, $A_R)
                        .select($A_R__, Add)
                        .derive(Add, plus, E)
                        .derive($Eps)
                        .$();
            }
        };

        var inModel = BnfParser.parseBnf(g.bnf()).model();
        var expectedModel = BnfParser.parseBnf(expectedG.bnf()).model();

        var nodeFactory = new LocalNodeFactory();

        var newModel = removeLeftRecursion(inModel, nodeFactory, eqOn(GrammarNode::name));

        assertTrue(structEquals(expectedModel, newModel, eqOn(GrammarNode::name)));
    }

    @Test
    public void test02() {
        var g = new AbstractGrammar() {
            Variable E, N, C, Add, Var, Add1, Y;
            Terminal one, y, plus, x;

            BNF bnf() {
                return start(E)
                        .select(E, N, C)
                        .derive(N, one)
                        .select(C, Add, Var)
                        .derive(Var, x)
                        .derive(Add, Add1, plus, E)
                        .select(Add1, E, Y)
                        .derive(Y, y)
                        .$();
            }
        };

        // Cannot be used due to malformed definition -- multiple rules with same LHS.
        // TODO: Come up with a better way for testing.
        // var expectedG = new AbstractGrammar() {
        //     Variable E, N, C, Add, Var, Y, $A_L, $A_R, $Eps, $A_R_, $A_R__;
        //     Terminal one, y, plus, x;
        //
        //     BNF bnf() {
        //         return start(E)
        //                 .derive(E, $A_L, $A_R)
        //                 .select($A_L, N, Add, Var)
        //                 .derive(N, one)
        //                 .derive(Var, x)
        //                 .select(Add, Add)
        //                 .derive(Add, Y, plus, E)
        //                 .derive(Y, y)
        //                 .select($A_R, $Eps, $A_R_)
        //                 .derive($A_R_, $A_R__, $A_R)
        //                 .select($A_R__, Add)
        //                 .select(Add, Add)
        //                 .derive(Add, plus, E)
        //                 .$();
        //     }
        // };

        var inModel = BnfParser.parseBnf(g.bnf()).model();
        // var expectedModel = BnfParser.parseBnf(expectedG.bnf()).model();

        var newModel = removeLeftRecursion(inModel, new LocalNodeFactory(), eqOn(GrammarNode::name));
    }

    @Test
    public void useless_left_recursion_01() {
        var g = new AbstractGrammar() {
            Variable E, N;
            Terminal one;

            BNF bnf() {
                return start(E)
                        .select(E, N, E)
                        .derive(N, one)
                        .$();
            }
        };

        // E -> E is useless, leaf E is removed.
        var eg = new AbstractGrammar() {
            Variable E, N, $A_L;
            Terminal one;

            BNF bnf() {
                return start(E)
                        .derive(E, $A_L)
                        .select($A_L, N)
                        .derive(N, one)
                        .$();
            }
        };

        var inModel = BnfParser.parseBnf(g.bnf()).model();
        var expectedModel = BnfParser.parseBnf(eg.bnf()).model();

        var newModel = removeLeftRecursion(inModel, new LocalNodeFactory(), eqOn(GrammarNode::name));

        assertTrue(structEquals(expectedModel, newModel, eqOn(GrammarNode::name)));
    }

    @Test
    public void useless_left_recursion_02() {
        var g = new AbstractGrammar() {
            Variable E, N, E1, C, Var;
            Terminal one, x;

            BNF bnf() {
                return start(E)
                        .select(E, N, E1, C)
                        .derive(N, one)
                        .derive(E1, E)
                        .select(C, Var, E)
                        .derive(Var, x)
                        .$();
            }
        };

        // C -> E is useless, C is inlined, leaf E is removed.
        var eg = new AbstractGrammar() {
            Variable E, N, $A_L, Var;
            Terminal one, x;

            BNF bnf() {
                return start(E)
                        .derive(E, $A_L)
                        .select($A_L, N, Var)
                        .derive(Var, x)
                        .derive(N, one)
                        .$();
            }
        };

        var inModel = BnfParser.parseBnf(g.bnf()).model();
        var expectedModel = BnfParser.parseBnf(eg.bnf()).model();

        var newModel = removeLeftRecursion(inModel, new LocalNodeFactory(), eqOn(GrammarNode::name));

        assertTrue(structEquals(expectedModel, newModel, eqOn(GrammarNode::name)));
    }

    static class LocalNodeFactory extends TestNodeFactory {

        @Override
        public GrammarNode.Free newFreeNode(CharSequence nameHint) {
            var name = nameHint instanceof LeftRecursion.NameHints ? "$" + nameHint : nameHint;
            return new GrammarNode.Free(name);
        }

        @Override
        public GrammarNode.Full newNode(CharSequence nameHint) {
            var name = nameHint instanceof LeftRecursion.NameHints ? "$" + nameHint : nameHint;
            return new GrammarNode.Full(name);
        }

    }

}
