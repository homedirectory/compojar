package compojar.bnf;

import compojar.gen.Namer;

import java.util.List;

import static compojar.bnf.Symbol.terminal;

public class EmptyProductionElimination {

    private final Namer namer;

    public EmptyProductionElimination(final Namer namer) {
        this.namer = namer;
    }

    public BNF apply(final BNF bnf) {
        return bnf.updateRules(rule -> rule instanceof Derivation derivation && derivation.rhs().isEmpty()
                ? new Derivation(derivation.lhs(), List.of(terminal(namer.emptyTerminalName())))
                : rule);
    }

}
