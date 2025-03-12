package compojar.util;

import java.util.function.*;
import java.util.stream.Stream;

public record T2<A, B> (A fst, B snd) {

    public static <X, Y> T2<X, Y> t2(X x, Y y) {
        return new T2<>(x, y);
    }

    public <X> X map(BiFunction<? super A, ? super B, X> fn) {
        return fn.apply(fst, snd);
    }

    public static <A, B, X> Function<T2<A, B>, X> _map(BiFunction<? super A, ? super B, X> fn) {
        return t2 -> t2.map(fn);
    }

    public boolean test(BiPredicate<? super A, ? super B> test) {
        return test.test(fst, snd);
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

    public static <X> Stream<X> stream(T2<? extends X, ? extends X> pair) {
        return Stream.of(pair.fst, pair.snd);
    }

    public T2<B, A> swap() {
        return new T2<>(snd, fst);
    }

    @Override
    public String toString() {
        return "(%s, %s)".formatted(fst, snd);
    }

}
