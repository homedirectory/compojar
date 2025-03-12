package compojar.model;

import compojar.bnf.AbstractGrammar;
import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static compojar.bnf.BnfBuilder.start;
import static compojar.bnf.Symbol.testOnValue;
import static compojar.model.CommonPrefix.*;
import static compojar.model.Keys.PARENT;
import static compojar.model.TestUtils.id;
import static compojar.util.T2.t2;
import static compojar.util.Util.permuteInPairs;
import static compojar.util.Util.remove;
import static org.assertj.core.api.Assertions.assertThat;

public class CommonPrefixTest {

    @Test
    public void common_prefix_leaf_01() {
        var bnf = new AbstractGrammar() {
            Variable E, Add, Mul, N;
            Terminal plus, mul, one;

            BNF bnf() {
                return start(E)
                        .select(E, Add, Mul)
                        .derive(Add, N.with(id, "N1"), plus, N)
                        .derive(Mul, N.with(id, "N2"), mul, N)
                        .derive(N, one)
                        .$();
            }
        }.bnf();

        var result = BnfParser.parseBnf(bnf);
        var model = result.model();
        GrammarNode N1 = result.findNode(testOnValue(id, "N1")),
                N2 = result.findNode(testOnValue(id, "N2"));

        var commonPrefix = findCommonPrefix(model, model.root(), GrammarNode::name);
        assertThat(commonPrefix)
                .isEqualTo(Set.of(N1, N2));
    }

    @Test
    public void common_prefix_leaf_02() {
        var bnf = new AbstractGrammar() {
            Variable E, Op, Var, Add, Mul, N;
            Terminal plus, mul, one, x;

            BNF bnf() {
                return start(E)
                        .select(E, Op, Var)
                        .select(Op, Add, Mul)
                        .derive(Add, N.with(id, "N1"), plus, N)
                        .derive(Mul, N.with(id, "N2"), mul, N)
                        .derive(N, one)
                        .derive(Var, x)
                        .$();
            }
        }.bnf();

        var result = BnfParser.parseBnf(bnf);
        var model = result.model();

        var N1 = result.findNode(testOnValue(id, "N1"));
        var N2 = result.findNode(testOnValue(id, "N2"));

        var commonPrefix = findCommonPrefix(model, model.root(), GrammarNode::name);
        assertThat(commonPrefix)
                .isEqualTo(Set.of(N1, N2));
    }

    @Test
    public void nearest_common_prefix_is_chosen_01() {
        var bnf = new AbstractGrammar() {
            Variable E, Add, Mul, N;
            Terminal plus, mul, one;

            BNF bnf() {
                return start(E)
                        .select(E, Add, Mul)
                        .derive(Add, N.with(id, "N1"), plus, N)
                        .derive(Mul, N.with(id, "N2"), mul, N)
                        .derive(N, one)
                        .$();
            }
        }.bnf();

        var result = BnfParser.parseBnf(bnf);
        var model = result.model();

        var N1 = result.findNode(testOnValue(id, "N1"));
        var N2 = result.findNode(testOnValue(id, "N2"));

        var commonPrefix = findCommonPrefix(model, GrammarNode::name);
        assertThat(commonPrefix)
                .isEqualTo(Set.of(N1, N2));
    }

    @Test
    public void common_parent_01() {
        var g = new AbstractGrammar() {
            Variable E, Add, Mul, N;
            Terminal plus, mul, one;

            BNF bnf() {
                return start(E)
                        .select(E, Add, Mul)
                        .derive(Add, N.with(id, "N1"), plus, N)
                        .derive(Mul, N.with(id, "N2"), mul, N)
                        .derive(N, one)
                        .$();
            }
        };

        var result = BnfParser.parseBnf(g.bnf());
        var model = result.model();

        GrammarNode
                E = result.nodeFor(g.E),
                Add = result.nodeFor(g.Add),
                Mul = result.nodeFor(g.Mul),
                plus = result.nodeFor(g.plus),
                mul = result.nodeFor(g.mul),
                N1 = result.findNode(testOnValue(id, "N1")),
                N2 = result.findNode(testOnValue(id, "N2")),
                N1_one = model.findNode(node -> model.get(node, PARENT).filter(N1::equals).isPresent()).get(),
                N2_one = model.findNode(node -> model.get(node, PARENT).filter(N2::equals).isPresent()).get()
                        ;

        assertThat(permuteInPairs(List.of(Add, Mul, N1, N2)))
                .allSatisfy(pair -> pair.run((node1, node2) -> assertThat(commonAncestor(model, node1, node2)).hasValue(E)));

        assertThat(permuteInPairs(List.of(Add, Mul, N1, N2_one)))
                .allSatisfy(pair -> pair.run((node1, node2) -> assertThat(commonAncestor(model, node1, node2)).hasValue(E)));

        assertThat(permuteInPairs(List.of(Add, Mul, N2, N1_one)))
                .allSatisfy(pair -> pair.run((node1, node2) -> assertThat(commonAncestor(model, node1, node2)).hasValue(E)));

        assertThat(permuteInPairs(List.of(Add, Mul, N1_one, N2_one)))
                .allSatisfy(pair -> pair.run((node1, node2) -> assertThat(commonAncestor(model, node1, node2)).hasValue(E)));

        assertThat(permuteInPairs(List.of(Add, Mul, N1_one, N2_one)))
                .allSatisfy(pair -> pair.run((node1, node2) -> assertThat(commonAncestor(model, node1, node2)).hasValue(E)));

        var nodesWithoutRoot = remove(model.nodes(), model.root());

        assertThat(nodesWithoutRoot.stream().flatMap(node -> Stream.of(t2(node, model.root()), t2(model.root(), node))))
                .allSatisfy(pair -> pair.run((node1, node2) -> assertThat(commonAncestor(model, node1, node2)).isEmpty()));

        assertThat(commonAncestor(model, N1_one, N1))
                .hasValue(Add);
        assertThat(commonAncestor(model, N2_one, N2))
                .hasValue(Mul);

        assertThat(model.nodes().stream().flatMap(node -> Stream.of(t2(node, plus), t2(plus, node))))
                .allSatisfy(pair -> pair.run((node1, node2) -> assertThat(commonAncestor(model, node1, node2)).isEmpty()));

        assertThat(model.nodes().stream().flatMap(node -> Stream.of(t2(node, mul), t2(plus, mul))))
                .allSatisfy(pair -> pair.run((node1, node2) -> assertThat(commonAncestor(model, node1, node2)).isEmpty()));
    }

    @Test
    public void common_prefix_elimination_01() {
        var bnf = new AbstractGrammar() {
            Variable E, E1, Sub, N, Var, E2, E3, Add, Frac, Neg;
            Terminal one, plus, minus, sqrt, div, x;

            BNF bnf() {
                return start(E)
                        .select(E, E1, E2)
                        .select(E1, Var, Sub)
                        .derive(Var, x)
                        .derive(Sub, N.with(id, "Sub_N1"), minus, N)
                        .derive(E2, E3, sqrt)
                        .select(E3, Add, Neg)
                        .derive(Add, Frac, plus, N)
                        .derive(Frac, N.with(id, "Frac_N1"), div, N)
                        .derive(Neg, minus, E)
                        .derive(N, one)
                        .$();
            }

        }.bnf();

        var result = BnfParser.parseBnf(bnf);
        var model = result.model();
        GrammarNode Sub_N1 = result.findNode(testOnValue(id, "Sub_N1")),
                Frac_N1 = result.findNode(testOnValue(id, "Frac_N1"));

        assertThat(findCommonPrefix(model, GrammarNode::name))
                .containsExactlyInAnyOrder(Sub_N1, Frac_N1);

        var newModel = removeCommonPrefix(model, GrammarNode::name, new TestNodeFactory());

        assertThat(findCommonPrefix(newModel, GrammarNode::name))
                .isEmpty();
    }

    @Test
    public void common_prefix_elimination_02() {
        var bnf = new AbstractGrammar() {
            Variable E, E1, Sub, N, Var, E2, E3, Add, Frac, Neg;
            Terminal one, plus, minus, sqrt, div, x;

            BNF bnf() {
                return start(E)
                        .select(E, E1, E2, N.with(id, "E_N"))
                        .select(E1, Var, Sub)
                        .derive(Var, x)
                        .derive(Sub, N.with(id, "Sub_N1"), minus, N)
                        .derive(E2, E3, sqrt)
                        .select(E3, Add, Neg)
                        .derive(Add, Frac, plus, N)
                        .derive(Frac, N.with(id, "Frac_N1"), div, N)
                        .derive(Neg, minus, E)
                        .derive(N, one)
                        .$();
            }

        }.bnf();

        var result = BnfParser.parseBnf(bnf);
        var model = result.model();
        GrammarNode Sub_N1 = result.findNode(testOnValue(id, "Sub_N1")),
                Frac_N1 = result.findNode(testOnValue(id, "Frac_N1")),
                E_N = result.findNode(testOnValue(id, "E_N"));

        assertThat(findCommonPrefix(model, GrammarNode::name))
                .containsExactlyInAnyOrder(Sub_N1, Frac_N1, E_N);

        var newModel = removeCommonPrefix(model, GrammarNode::name, new TestNodeFactory());

        assertThat(findCommonPrefix(newModel, GrammarNode::name))
                .isEmpty();
    }

    /**
     * Common prefix in a sub-tree.
     * Expect only the sub-tree to be affected.
     */
    @Test
    public void common_prefix_elimination_03() {
        var bnf = new AbstractGrammar() {
            Variable E, E1, Sub, N, Var, E2, E3, Add, Frac, Neg;
            Terminal one, plus, minus, sqrt, div, x;

            BNF bnf() {
                return start(E)
                        .select(E, E1, E2, N.with(id, "E_N"))
                        .select(E1, Var)
                        .derive(Var, x)
                        .derive(E2, E3, sqrt)
                        .select(E3, Add, Sub, Neg)
                        .derive(Add, Frac, plus, N)
                        .derive(Frac, N.with(id, "Frac_N1"), div, N)
                        .derive(Sub, N.with(id, "Sub_N1"), minus, N)
                        .derive(Neg, minus, E)
                        .derive(N, one)
                        .$();
            }

        }.bnf();

        var result = BnfParser.parseBnf(bnf);
        var model = result.model();
        GrammarNode Sub_N1 = result.findNode(testOnValue(id, "Sub_N1")),
                Frac_N1 = result.findNode(testOnValue(id, "Frac_N1")),
                E_N = result.findNode(testOnValue(id, "E_N"));

        assertThat(findCommonPrefix(model, GrammarNode::name))
                .containsExactlyInAnyOrder(Sub_N1, Frac_N1, E_N);

        var newModel = removeCommonPrefix(model, GrammarNode::name, new TestNodeFactory());

        assertThat(findCommonPrefix(newModel, GrammarNode::name))
                .isEmpty();
    }

}
