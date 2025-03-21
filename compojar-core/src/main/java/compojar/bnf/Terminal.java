package compojar.bnf;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparing;

public non-sealed interface Terminal extends Symbol {

    Comparator<Terminal> comparator = comparing(t -> t.name().toString());

    @Override
    default <T> Terminal with(Key<T> key, T value) {
        return new TerminalRecord(name(), List.of()).with(key, value);
    }

    @Override
    default Symbol normalise() {
        return new TerminalRecord(name(), List.of());
    }

    default List<Parameter> getParameters() {
        return List.of();
    }

    default boolean hasParameters() {
        return !getParameters().isEmpty();
    }

    default Terminal parameters(Type type, CharSequence name) {
        return new TerminalRecord(name(), List.of(new Parameter(type, name)));
    }

    default Terminal parameters(Type type1, CharSequence name1, Type type2, CharSequence name2) {
        return parameters(type1, name1).parameters(type2, name2);
    }

}
