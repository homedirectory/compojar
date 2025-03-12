package compojar.model;

import compojar.bnf.AbstractGrammar;
import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;
import compojar.model.GrammarNode.Free;
import compojar.model.GrammarNode.Full;
import compojar.model.GrammarNode.Leaf;
import org.junit.Test;

import java.util.Set;

import static compojar.bnf.BnfBuilder.start;
import static compojar.bnf.Rule.derivation;
import static compojar.bnf.Rule.selection;
import static compojar.model.Eq.eqOn;
import static compojar.model.Keys.NEXT;
import static compojar.model.Keys.PARENT;
import static compojar.model.TestUtils.grammarEq;
import static org.assertj.core.api.Assertions.assertThat;

public class InlineTest {

    @Test
    public void inline_node_whose_parent_is_free_node() {
        class InputGrammar extends AbstractGrammar {
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
        }

        var inGrammar = new InputGrammar();
        var inModel = BnfParser.parseBnf(inGrammar.bnf()).model();

        class ExpectedGrammar extends AbstractGrammar {
            Variable E, X, Y;
            Terminal x, y;

            BNF bnf() {
                return new BNF(
                        Set.of(selection(E, X, Y),
                               derivation(X, x),
                               derivation(Y, y)),
                        E);
            }
        }

        var expectedGrammar = new ExpectedGrammar();
        var expectedModel = BnfParser.parseBnf(expectedGrammar.bnf()).model();

        var inlinedModel = Inline.inline(inModel, new TestNodeFactory());

        assertThat(inlinedModel)
                .usingEquals(grammarEq(eqOn(GrammarNode::name)))
                .isEqualTo(expectedModel);
    }

    // TODO: Refactor using semantic comparison.
    @Test
    public void inline_node_whose_parent_is_regular_node() {
        GrammarNode E = new Free("E"),
                E1 = new Leaf("E1"),
                E2 = new Full("E2"),
                E3 = new Free("E3"),
                sqrt = new Leaf("sqrt"),
                Add = new Full("Add"),
                Neg = new Leaf("Neg"),
                plus = new Leaf("+");
                        ;
        var model = new GrammarTreeModel(Set.of(E, E1, E2, E3, sqrt, Add, Neg, plus), E)
                .set(E1, PARENT, E)
                .set(E2, PARENT, E)
                .set(E3, PARENT, E2)
                .set(E3, NEXT, sqrt)
                .set(Add, PARENT, E3)
                .set(plus, PARENT, Add)
                .set(Neg, PARENT, E3)
                ;

        var nodeFactory = new TestNodeFactory() {
            @Override
            public Full newNode(CharSequence nameHint) {
                return new Full("$" + nameHint);
            }

            @Override
            public Free newFreeNode(CharSequence nameHint) {
                return new Free("$" + nameHint);
            }
        };

        var inlinedModel = Inline.inline(model, nodeFactory);

        assertThat(inlinedModel.nodes())
                .extracting(GrammarNode::name)
                .containsExactlyInAnyOrder("E", "E1", "$E2", "$E2", "sqrt", "sqrt", "Add", "Neg", "+");

        assertThat(inlinedModel.requireAttribute(E1, PARENT)).isEqualTo(E);
        assertThat(inlinedModel.requireAttribute(plus, PARENT)).isEqualTo(Add);

        final var _E2_of_Add = inlinedModel.requireAttribute(Add, PARENT);
        final var _E2_of_Neg = inlinedModel.requireAttribute(Neg, PARENT);
        assertThat(inlinedModel.requireAttribute(_E2_of_Add, PARENT))
                .isEqualTo(E);
        assertThat(inlinedModel.requireAttribute(_E2_of_Neg, PARENT))
                .isEqualTo(E);
        assertThat(inlinedModel.requireAttribute(Add, NEXT))
                .satisfies(it -> assertThat(it.name()).isEqualTo("sqrt"));
        assertThat(inlinedModel.requireAttribute(Neg, NEXT))
                .satisfies(it -> assertThat(it.name()).isEqualTo("sqrt"));
    }

    @Test
    public void inline_recursively() {
        GrammarNode E = new Free("E"),
                E1 = new Full("E1"),
                E2 = new Free("E2"),
                E2a = new Free("E2a"),
                E2a1 = new Leaf("E2a1"),
                E2a2 = new Leaf("E2a2"),
                E2b = new Leaf("E2b"),
                E3 = new Free("E3"),
                sqrt = new Leaf("sqrt"),
                Sub = new Free("Sub"),
                Sub1 = new Leaf("Sub1"),
                Var = new Leaf("Var")
                ;
        var nodes = Set.of(E, E1, E2, E2a, E2a1, E2a2, E2b, E3, sqrt, Sub, Sub1, Var);
        var model = new GrammarTreeModel(nodes, E)
                .set(E1, PARENT, E)
                .set(E2, PARENT, E)
                .set(E2a, PARENT, E2)
                .set(E2b, PARENT, E2)
                .set(E2a1, PARENT, E2a)
                .set(E2a2, PARENT, E2a)
                .set(E3, PARENT, E1)
                .set(E3, NEXT, sqrt)
                .set(Sub, PARENT, E3)
                .set(Sub1, PARENT, Sub)
                .set(Var, PARENT, E3)
                ;

        var nodeFactory = new TestNodeFactory() {
            @Override
            public Full newNode(CharSequence nameHint) {
                return new Full("$" + nameHint);
            }

            @Override
            public Free newFreeNode(CharSequence nameHint) {
                return new Free("$" + nameHint);
            }
        };

        var inlinedModel = Inline.inline(model, nodeFactory);

        assertThat(inlinedModel.nodes())
                .extracting(GrammarNode::name)
                .containsExactlyInAnyOrder("E", "E2b", "E2a1", "E2a2", "$E1", "$E1", "sqrt", "sqrt", "Sub1", "Var");

        assertThat(inlinedModel.requireAttribute(E2b, PARENT)).isEqualTo(E);
        assertThat(inlinedModel.requireAttribute(E2a1, PARENT)).isEqualTo(E);

        var _E1_of_Sub1 = inlinedModel.requireAttribute(Sub1, PARENT);
        var _E1_of_Var = inlinedModel.requireAttribute(Var, PARENT);
        assertThat(inlinedModel.requireAttribute(_E1_of_Sub1, PARENT)).isEqualTo(E);
        assertThat(inlinedModel.requireAttribute(_E1_of_Var, PARENT)).isEqualTo(E);

        assertThat(inlinedModel.requireAttribute(Sub1, NEXT)).extracting(GrammarNode::name).isEqualTo("sqrt");
        assertThat(inlinedModel.requireAttribute(Var, NEXT)).extracting(GrammarNode::name).isEqualTo("sqrt");
    }

}
