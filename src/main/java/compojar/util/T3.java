package compojar.util;

public record T3<A, B, C> (A fst, B snd, C thd) {

    public static <X, Y, Z> T3<X, Y, Z> t3(X x, Y y, Z z) {
        return new T3<>(x, y, z);
    }

    public <X> X map(Function3<? super A, ? super B, ? super C, X> fn) {
        return fn.apply(fst, snd, thd);
    }

}
