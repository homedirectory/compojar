package compojar.bnf;

import java.util.Comparator;

import static java.util.Comparator.comparing;

public non-sealed interface Variable extends Symbol {

    Comparator<Variable> comparator = comparing(v -> v.name().toString());
}
