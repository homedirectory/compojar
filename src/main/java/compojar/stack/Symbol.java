package compojar.stack;

import compojar.bnf.Parameter;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;

import java.util.List;

public interface Symbol extends CharSequence  {

    Symbol empty = symbol("<empty>");

    static Symbol symbol(CharSequence name) {
        return new SymbolRecord(name, List.of());
    }

    static Symbol symbol(compojar.bnf.Symbol bnfSymbol) {
        return switch (bnfSymbol) {
            case Terminal terminal -> new SymbolRecord(terminal.name(), terminal.getParameters());
            case Variable variable -> new SymbolRecord(variable.name(), List.of());
        };
    }

    default CharSequence name() {
        return this instanceof Enum<?> e ? e.name() : toString();
    }

    default List<Parameter> parameters() {
        return List.of();
    }

    default boolean hasParameters() {
        return !parameters().isEmpty();
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
