package compojar.stack;

public interface Symbol extends CharSequence  {

    Symbol empty = new StandardSymbol("<empty>");

    static Symbol symbol(CharSequence name) {
        var trueName = name instanceof compojar.bnf.Symbol s ? s.name() : name;
        return new StandardSymbol(trueName);
    }

    default CharSequence name() {
        return this instanceof Enum<?> e ? e.name() : toString();
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
