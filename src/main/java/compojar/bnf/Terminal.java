package compojar.bnf;

import java.util.Comparator;

import static java.util.Comparator.comparing;

public non-sealed interface Terminal extends Symbol {

    Comparator<Terminal> comparator = comparing(t -> t.name().toString());

}
