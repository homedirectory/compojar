package compojar.bnf;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public sealed interface Symbol
        extends CharSequence
        permits Terminal, Variable
{

    static Terminal terminal(CharSequence name) {
        return new TerminalRecord(name, List.of());
    }

    static Variable variable(CharSequence name) {
        return new StandardVariable(name);
    }

    default CharSequence name() {
        return this instanceof Enum<?> e ? e.name() : toString();
    }

    /**
     * Strip the symbol to its bare essentials.
     */
    Symbol normalise();

    default boolean normalEquals(Symbol symbol) {
        return this == symbol || normalise().equals(symbol.normalise());
    }

    /**
     * Returns an annotated version of this symbol using the specified key and value.
     */
    <T> Symbol with(Key<T> key, T value);

    default <T> T get(Key<T> key) {
        throw new UnsupportedOperationException("TODO");
    }

    default <T> Optional<T> getOpt(Key<T> key) {
        return Optional.empty();
    }

    default boolean has(Key<?> key) {
        return false;
    }

    static <T> Predicate<? super Symbol> testOnValue(Key<T> key, T value) {
        return node -> node.getOpt(key).filter(v -> Objects.equals(v, value)).isPresent();
    }

    @Override
    default int length() {
        return name().length();
    }

    @Override
    default char charAt(int index) {
        return name().charAt(index);
    }

    @Override
    default CharSequence subSequence(int start, int end) {
        return name().subSequence(start, end);
    }

}
