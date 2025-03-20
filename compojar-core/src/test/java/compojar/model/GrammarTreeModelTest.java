package compojar.model;

import compojar.bnf.AbstractGrammar;
import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;
import org.junit.Test;

import java.util.Set;

import static compojar.bnf.BnfBuilder.start;
import static compojar.bnf.Symbol.testOnValue;
import static compojar.model.Keys.*;
import static compojar.model.TestUtils.id;
import static org.assertj.core.api.Assertions.assertThat;

public class GrammarTreeModelTest {

    @Test
    public void subtree_rooted_at_leaf_with_target() {
        var g = new AbstractGrammar() {
            Variable E;
            Terminal begin;

            BNF bnf() {
                return start(E)
                        .derive(E, begin, E.with(id, "E_leaf"))
                        .$();
            }
        };

        var result = BnfParser.parseBnf(g.bnf());
        var inModel = result.model();

        var E_leaf = result.findNode(testOnValue(id, "E_leaf"));

        var subtree = inModel.subtree(E_leaf);

        assertThat(subtree.nodes()).isEqualTo(Set.of(E_leaf));
        assertThat(subtree.get(E_leaf, PARENT)).isEmpty();
        assertThat(subtree.get(E_leaf, TARGET)).isEmpty();
        assertThat(subtree.get(E_leaf, NEXT)).isEmpty();
        assertThat(subtree.get(E_leaf, CHILDREN)).isEmpty();
    }
}
