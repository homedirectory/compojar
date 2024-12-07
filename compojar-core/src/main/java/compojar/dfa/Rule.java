package compojar.dfa;

public record Rule (String source, String symbol, String destination) {

    // @Override
    // public String apply(final String state, final String symbol) {
    //     if (source.equals(state) && this.symbol.equals(symbol)) {
    //         return destination;
    //     }
    //     else {
    //         throw new IllegalStateException("Rule doesn't apply! Rule: %s; state: %s; symbol: %s".formatted(this, state, symbol));
    //     }
    // }

}
