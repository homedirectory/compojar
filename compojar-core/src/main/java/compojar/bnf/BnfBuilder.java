package compojar.bnf;

import java.util.ArrayList;
import java.util.List;

//  start(S)
// .select(S, X, Y)
// .derive(X, x)
// .$()

public final class BnfBuilder {

    public static BnfBuilder start(Variable v) {
        return new BnfBuilder(v);
    }

    private final Variable start;
    private final List<Rule> rules;

    BnfBuilder(final Variable start) {
        this.start = start;
        rules = new ArrayList<>();
    }

    public BnfBuilder select(Variable lhs, Variable rhsHd, Variable... rhsRest) {
        rules.add(Rule.selection(lhs, rhsHd, rhsRest));
        return this;
    }

    public BnfBuilder derive(Variable lhs, Symbol... rhs) {
        rules.add(Rule.derivation(lhs, rhs));
        return this;
    }

    public BNF $() {
        return new BNF(rules, start);
    }

}
