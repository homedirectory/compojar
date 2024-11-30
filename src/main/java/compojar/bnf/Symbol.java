package compojar.bnf;

import java.util.Optional;

public sealed interface Symbol
        extends CharSequence
        permits Terminal, Variable
{

    static Terminal terminal(CharSequence name) {
        return new StandardTerminal(name);
    }

    static Variable variable(CharSequence name) {
        return new StandardVariable(name);
    }

    default CharSequence name() {
        return this instanceof Enum<?> e ? e.name() : toString();
    }

    default <T> T get(Key<T> key) {
        throw new UnsupportedOperationException("TODO");
    }

    default <T> Optional<T> getOpt(Key<T> key) {
        return Optional.empty();
    }

    default boolean has(Key<?> key) {
        return false;
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
