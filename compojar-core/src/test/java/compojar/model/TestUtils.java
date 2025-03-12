package compojar.model;

import static compojar.model.Eq.eqOn;
import static compojar.model.Eq.eqSet;
import static compojar.model.GrammarTreeModel.eqAttribute;

public final class TestUtils {

    /** BNF metadata key to identify symbols. */
    public static final compojar.bnf.Key<Object> id = new compojar.bnf.Key<>() {};

    /**
     * Structural equality of grammars.
     */
    public static Eq<GrammarTreeModel> grammarEq(Eq<GrammarNode> eqNode) {
        return eqOn(GrammarTreeModel::root, eqNode)
                .and(eqOn(GrammarTreeModel::nodes, eqSet(eqNode)))
                .and(eqAttribute(Keys.PARENT, eqNode))
                .and(eqAttribute(Keys.NEXT, eqNode));
    }

    private TestUtils() {}

}
