package compojar.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

@FunctionalInterface
public interface Eq<T> extends BiPredicate<T, T> {

    boolean areEqual(T a, T b);

    @Override
    default boolean test(T t1, T t2) {
        return areEqual(t1, t2);
    }

    default Eq<T> and(Eq<T> eq) {
        return (a, b) -> Eq.this.areEqual(a, b) && eq.areEqual(a, b);
    }

    default Eq<T> or(Eq<T> eq) {
        return (a, b) -> Eq.this.areEqual(a, b) || eq.areEqual(a, b);
    }

    static <T> Eq<T> eq() {
        return Objects::equals;
    }

    static <T, U> Eq<T> eqOn(Function<T, U> fn, Eq<? super U> eqU) {
        return (t1, t2) -> eqU.areEqual(fn.apply(t1), fn.apply(t2));
    }

    static <T, U> Eq<T> eqOn(Function<T, U> fn) {
        return eqOn(fn, eq());
    }

    static <T> Eq<Set<T>> eqSet(Eq<T> eqElement) {
        return (set1, set2) ->
                set1.size() == set2.size()
                && set1.stream().allMatch(elt1 -> set2.stream().anyMatch(elt2 -> eqElement.areEqual(elt1, elt2)));
    }

    static <K,V> Eq<Map<K,V>> eqMap(Eq<K> eqKey, Eq<V> eqVal) {
        return (map1, map2) -> {
            if (map1.size() != map2.size()) {
                return false;
            }

            return map1.keySet().stream()
                .allMatch(key1 -> map2.keySet().stream()
                                      .anyMatch(key2 -> eqKey.areEqual(key1, key2) && eqVal.areEqual(map1.get(key1), map2.get(key2))));
        };
    }

    static <K,V> Eq<Map<K,V>> eqMapAt(K key, Eq<V> eqVal) {
        return (map1, map2) ->
                map1.containsKey(key) && map2.containsKey(key)
                        ? eqVal.areEqual(map1.get(key), map2.get(key))
                        : !map1.containsKey(key) && !map2.containsKey(key);
    }

}
