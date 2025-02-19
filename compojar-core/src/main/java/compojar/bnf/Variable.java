package compojar.bnf;

import java.util.Comparator;

import static java.util.Comparator.comparing;

public non-sealed interface Variable extends Symbol {

    Comparator<Variable> comparator = comparing(v -> v.name().toString());

    @Override
    default <T> Variable with(Key<T> key, T value) {
        return new StandardVariable(name(), Metadata.builder().put(key, value).build());
    }

    @Override
    default Symbol normalise() {
        return new StandardVariable(name());
    }

}
