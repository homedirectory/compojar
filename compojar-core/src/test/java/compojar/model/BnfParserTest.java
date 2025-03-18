package compojar.model;

import compojar.bnf.AbstractGrammar;
import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;
import org.junit.Test;

import java.util.Set;

import static compojar.bnf.BnfBuilder.start;
import static compojar.bnf.Symbol.testOnValue;
import static compojar.model.Keys.TARGET;
import static compojar.model.TestUtils.id;
import static compojar.util.Util.difference;
import static org.assertj.core.api.Assertions.assertThat;

public class BnfParserTest {

    @Test
    public void target_is_present_only_for_recursive_rules() {
        var g = new AbstractGrammar() {
            Variable E, N, Add, Neg;
            Terminal plus, one, minus;

            BNF bnf() {
                return start(E)
                        .select(E, N, Add, Neg)
                        .derive(N, one)
                        .derive(Add, E.with(id, "Add_E1"), plus, E.with(id, "Add_E2"))
                        .derive(Neg, minus, E.with(id, "Neg_E"))
                        .$();
            }
        };

        var result = BnfParser.parseBnf(g.bnf());
        var inModel = result.model();

        GrammarNode E = result.nodeFor(g.E),
                Add_E1 = result.findNode(testOnValue(id, "Add_E1")),
                Add_E2 = result.findNode(testOnValue(id, "Add_E2")),
                Neg_E = result.findNode(testOnValue(id, "Neg_E"));

        assertThat(inModel.get(Add_E1, TARGET)).hasValue(E);
        assertThat(inModel.get(Add_E2, TARGET)).hasValue(E);
        assertThat(inModel.get(Neg_E, TARGET)).hasValue(E);

        assertThat(difference(inModel.nodes(), Set.of(Add_E1, Add_E2, Neg_E)))
                .noneMatch(node -> inModel.has(node, TARGET));
    }

}
