package compojar.bnf;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

public record Derivation (Variable lhs, List<Symbol> rhs) implements Rule {

    public static Derivation derivation(Variable lhs, Symbol... rhs) {
        return new Derivation(lhs, List.of(rhs));
    }

    public static Derivation derivation(Variable lhs, List<? extends Symbol> rhs) {
        return new Derivation(lhs, new ArrayList<>(rhs));
    }

    @Override
    public boolean semanticEquals(final Rule rule) {
        return rule instanceof Derivation that
                && lhs.equals(that.lhs) && rhs.equals(that.rhs);
    }

    @Override
    public String toString() {
        return "%s ::= %s".formatted(
                lhs.name(),
                rhs.isEmpty() ? "<empty>" : rhs.stream().map(Rule::toString).collect(joining(" ")));
    }

}
