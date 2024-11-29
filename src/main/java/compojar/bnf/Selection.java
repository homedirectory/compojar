package compojar.bnf;

import compojar.util.Util;

import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

public record Selection (Variable lhs, List<Variable> rhs) implements Rule {

    public static Selection selection(Variable lhs, Variable rhs0, Variable... rhs) {
        return new Selection(lhs, Util.list(rhs0, rhs));
    }

    public Selection updateRhs(Function<? super List<Variable>, List<Variable>> fn) {
        return new Selection(lhs, fn.apply(rhs));
    }

    @Override
    public List<Variable> rhs() {
        return rhs;
    }

    @Override
    public boolean semanticEquals(final Rule rule) {
        return rule instanceof Selection that && lhs.equals(that.lhs)
                && new HashSet<>(rhs).equals(new HashSet<>(that.rhs));
    }

    @Override
    public String toString() {
        return "%s ::= %s".formatted(
                Rule.toString(lhs),
                rhs.stream().map(Rule::toString).collect(joining(" | ")));
    }

}
