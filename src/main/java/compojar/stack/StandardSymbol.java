package compojar.stack;

record StandardSymbol (CharSequence name) implements Symbol  {

    @Override
    public String toString() {
        return name.toString();
    }

}
