package compojar.stack;

public interface Symbol extends CharSequence  {

    Symbol empty = new StandardSymbol("<empty>");

    static Symbol symbol(CharSequence name) {
        return new StandardSymbol(name);
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
