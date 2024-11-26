package compojar.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public record T2<A, B> (A fst, B snd) {

    public static <X, Y> T2<X, Y> t2(X x, Y y) {
        return new T2<>(x, y);
    }

    public <X> X map(BiFunction<? super A, ? super B, X> fn) {
        return fn.apply(fst, snd);
    }

    // public <X, Y> flatMap(BiFunction<? super A, ? super B, T2<X, Y>> fn) {
    //     return fn.apply()
    // }

    public void run(BiConsumer<? super A, ? super B> action) {
        action.accept(fst, snd);
    }

    public <C> T2<A, C> map2(Function<? super B, C> fn) {
        return new T2<>(fst, fn.apply(snd));
    }

    public <C> T2<C, B> map1(Function<? super A, C> fn) {
        return new T2<>(fn.apply(fst), snd);
    }

    public T2<A, B> peek2(Consumer<? super B> action) {
        action.accept(snd);
        return this;
    }

}
