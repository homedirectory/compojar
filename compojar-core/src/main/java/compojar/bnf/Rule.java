package compojar.bnf;

import compojar.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public sealed interface Rule permits Derivation, Selection {

    static Derivation derivation(Variable lhs, Symbol... rhs) {
        return new Derivation(lhs, List.of(rhs));
    }

    static Derivation derivation(Variable lhs, List<? extends Symbol> rhs) {
        return new Derivation(lhs, new ArrayList<>(rhs));
    }

    static Selection selection(Variable lhs, Variable rhs0, Variable... rhs) {
        return new Selection(lhs, Util.list(rhs0, rhs));
    }

    Comparator<Rule> compareByLhs = comparing(Rule::lhs, Variable.comparator);

    Variable lhs();

    List<? extends Symbol> rhs();

    boolean semanticEquals(Rule rule);

    default Stream<Symbol> allSymbols() {
        return Stream.concat(Stream.of(lhs()), rhs().stream());
    }

    static String toString(final Symbol symbol) {
        return switch (symbol) {
            case Terminal t -> t.name().toString();
            case Variable v -> String.format("<%s>", v.name());
        };
    }

}
