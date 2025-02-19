package compojar.util;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static compojar.util.T2.t2;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.util.Collections.*;

public final class Util {

    public static boolean contentEquals(CharSequence cs1, CharSequence cs2) {
        if (cs1.length() != cs2.length()) {
            return false;
        }
        else {
            for (int i = 0; i < cs1.length(); i++) {
                if (cs1.charAt(i) != cs2.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static <K, V> Map<K, V> mapOf(K key, V value, Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Expected an even number of key-value arguments: %s".formatted(pairs.length));
        }

        var map = new HashMap<K, V>(2 + pairs.length);
        map.put(key, value);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((K) pairs[i], (V) pairs[i + 1]);
        }
        return unmodifiableMap(map);
    }

    @SafeVarargs
    public static <X> Set<X> concatSet(Collection<? extends X>... collections) {
        return Arrays.stream(collections).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @SafeVarargs
    public static <X> List<X> concatList(Collection<? extends X>... collections) {
        return Arrays.stream(collections).flatMap(Collection::stream).collect(Collectors.toList());
    }

    public static <X> List<X> append(final Collection<? extends X> xs, final X... rest) {
        final var list = new ArrayList<X>(xs);
        Collections.addAll(list, rest);
        return unmodifiableList(list);
    }

    @SafeVarargs
    public static <X> List<X> list(final X x0, final X... xs) {
        final var list = new ArrayList<X>(1 + xs.length);
        list.add(x0);
        Collections.addAll(list, xs);
        return unmodifiableList(list);
    }

    public static <X, Y> Stream<Y> enumeratedStream(final BaseStream<X, ?> stream,
                                                    final EnumeratedF<? super X, Y> f)
    {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(enumeratedIterator(stream.iterator(), f), 0), false);
    }

    private static <X, Y> Iterator<Y> enumeratedIterator(
            final Iterator<X> iterator,
            final EnumeratedF<? super X, Y> f)
    {
        return new Iterator<>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Y next() {
                return f.apply(iterator.next(), i++);
            }
        };
    }

    public static <X> X first(final Iterable<X> xs) {
        return xs instanceof SequencedCollection<X> c ? c.getFirst() : xs.iterator().next();
    }

    public static <X> Optional<X> firstOpt(final Iterable<X> xs) {
        return xs instanceof SequencedCollection<X> c
                ? c.isEmpty() ? Optional.empty() : Optional.of(c.getFirst())
                : xs.iterator().hasNext() ? Optional.of(xs.iterator().next()) : Optional.empty();
    }

    public static <X, Y, Z> Optional<Z> optionalMap2(Optional<X> xOpt, Optional<Y> yOpt, BiFunction<? super X, ? super Y, Z> fn) {
        return xOpt.flatMap(x -> yOpt.map(y -> fn.apply(x, y)));
    }

    public static <X> List<X> prepend(final X head, final List<? extends X> tail) {
        return tail.isEmpty() ? List.of(head) : concatList(List.of(head), tail);
    }

    public static <X> SequencedSet<X> sequencedSet(final X x0, final X... xs) {
        var set = new LinkedHashSet<X>(1 + xs.length);
        set.add(x0);
        Collections.addAll(set, xs);
        return unmodifiableSequencedSet(set);
    }

    public static <K, V> Map<K, V> mapRemoveAll(final Map<K, V> map, final List<K> keys) {
        if (keys.isEmpty()) {
            return map;
        }
        else {
            var newMap = new HashMap<K, V>(map);
            keys.forEach(newMap::remove);
            return unmodifiableMap(newMap);
        }
    }

    public static <K, V> Map<K, V> mapRemove(final Map<K, V> map, final K key) {
        if (!map.containsKey(key)) {
            return map;
        }
        else {
            var newMap = new HashMap<K, V>(map);
            newMap.remove(key);
            return unmodifiableMap(newMap);
        }
    }

    public static <K, V> Map<K, V> filterKeys(Map<K, V> map, BiPredicate<? super K, ? super V> test) {
        var newMap = new HashMap<K, V>(map.size() / 2);
        map.forEach((k, v) -> {
            if (test.test(k, v))
                newMap.put(k, v);
        });
        return unmodifiableMap(newMap);
    }

    public static <X> Collection<X> strictMerge(
            final Collection<? extends X> xs,
            final Collection<? extends X> ys,
            final BiPredicate<? super X, ? super X> test)
    {
        xs.stream().filter(x -> ys.stream().anyMatch(y -> test.test(x, y))).findFirst().ifPresent(elt -> {
            throw new IllegalArgumentException("Cannot merge two collections that contain a duplicate element: %s".formatted(elt));
        });

        return concatList(xs, ys);
    }

    public static <K, V> Map<K, V> mapStrictMerge(final Map<K, V> map1, final Map<K, V> map2) {
        map1.keySet().stream().filter(map2::containsKey).findFirst().ifPresent(key -> {
            throw new IllegalArgumentException("""
                                               Cannot merge two maps that contain a duplicate key: %s.
                                               Map 1 value: %s
                                               Map 2 value: %s
                                               """.formatted(key, map1.get(key), map2.get(key)));
        });

        var map = new HashMap<K, V>(map1.size() + map2.size());
        map.putAll(map1);
        map.putAll(map2);
        return unmodifiableMap(map);
    }

    /** Left-biased union. */
    public static <K, V> Map<K, V> mapUnion(Map<K, V> left, Map<K, V> right) {
        var union = new HashMap<K, V>(left.size() + right.size());
        union.putAll(right);
        union.putAll(left);
        return unmodifiableMap(union);
    }

    /** Right-biased union. */
    public static <K, V> Map<K, V> mapUnionRight(Map<K, V> left, Map<K, V> right) {
        return mapUnion(right, left);
    }

    public static <K1, V, K2> Map<K2, V> mapKeys(final Map<K1, V> map, final Function<? super K1, K2> fn) {
        var newMap = new HashMap<K2, V>(map.size());
        map.forEach((k, v) -> newMap.put(fn.apply(k), v));
        return unmodifiableMap(newMap);
    }

    public static <K, V> Map<K, V> insert(final Map<K, V> map, final K key, final V value)
    {
        var newMap = new HashMap<K, V>(map);
        newMap.put(key, value);
        return unmodifiableMap(newMap);
    }

    public static <K, V> Map<K, V> insertWith(Map<K, V> map, K key, V value, BinaryOperator<V> fn) {
        var newMap = new HashMap<>(map);
        newMap.merge(key, value, fn);
        return unmodifiableMap(newMap);
    }

    public static <K, V> Map<K,V> update(Map<K, V> map, K key, Function<? super V, ? extends V> fn) {
        if (!map.containsKey(key)) {
            return map;
        }
        else {
            var newMap = new HashMap<>(map);
            newMap.put(key, fn.apply(newMap.get(key)));
            return unmodifiableMap(newMap);
        }
    }

    public static <K, V> Map<K, V> removeAll(final Map<K, V> map, final BiPredicate<? super K, ? super V> test) {
        return map.entrySet().stream()
                .filter(entry -> !test.test(entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <X> Optional<T2<X, Integer>> findWithIndex(final List<X> xs, final Predicate<? super X> test) {
        for (int i = 0; i < xs.size(); i++) {
            if (test.test(xs.get(i))) {
                return Optional.of(t2(xs.get(i), i));
            }
        }
        return Optional.empty();
    }


    public static <X> Optional<T2<X, Integer>> findWithIndex2(final List<X> xs, final BiPredicate<? super X, ? super Integer> test) {
        for (int i = 0; i < xs.size(); i++) {
            if (test.test(xs.get(i), i)) {
                return Optional.of(t2(xs.get(i), i));
            }
        }
        return Optional.empty();
    }

    public static <X> List<X> replace(final List<X> xs, final int idx, final X item) {
        var newList = new ArrayList<X>(xs);
        newList.set(idx, item);
        return unmodifiableList(newList);
    }

    public static String decapitalise(CharSequence name) {
        if (name.isEmpty()) {
            return "";
        }
        else if (isUpperCase(name.charAt(0))) {
            var sb = new StringBuilder(name.length());
            sb.append(toLowerCase(name.charAt(0)));
            sb.append(name, 1, name.length());
            return sb.toString();
        }
        else {
            return name.toString();
        }
    }

    public static <X> Set<X> remove(Set<X> xs, X x) {
        if (!xs.contains(x)) {
            return xs;
        }
        else {
            var result = new HashSet<X>(xs);
            result.remove(x);
            return unmodifiableSet(result);
        }
    }

    @FunctionalInterface
    public interface EnumeratedF<X, Y> {

        Y apply(X x, int i);

    }

    public static <K, V> V requireKey(Map<K, V> map, K key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("Required key is missing: %s".formatted(key));
        }
        return map.get(key);
    }

    public static <K, V> V requireKey(Map<K, V> map, K key, Function<? super K, ? extends CharSequence> messageFn) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException(messageFn.apply(key).toString());
        }
        return map.get(key);
    }

    public static <X> List<X> subList(final List<X> xs, final int start) {
        return xs.subList(start, xs.size());
    }

    public static <A, B, K, V> Map<K,V> mapFromPairs(final Collection<T2<A, B>> pairs,
                                                     final BiFunction<? super A, ? super B, K> keyFn,
                                                     final BiFunction<? super A, ? super B, V> valueFn)
    {
        return pairs.stream()
                .collect(Collectors.toMap(pair -> pair.map(keyFn), pair -> pair.map(valueFn)));
    }

    public static <A, B, C, K, V> Map<K,V> mapFromTriples(final Collection<T3<A, B, C>> pairs,
                                                       final Function3<? super A, ? super B, ? super C, K> keyFn,
                                                       final Function3<? super A, ? super B, ? super C, V> valueFn)
    {
        return pairs.stream().collect(Collectors.toMap(triple -> triple.map(keyFn), triple -> triple.map(valueFn)));
    }

    public static <X> Optional<X> find(Collection<X> xs, Predicate<? super X> test) {
        return xs.stream().filter(test).findFirst();
    }

    public static <X> boolean allElementsEqual(Iterable<X> xs, Function<? super X, ?> classifier) {
        final var iterator = xs.iterator();
        if (!iterator.hasNext())
            return true;
        else {
            var item = classifier.apply(iterator.next());
            while (iterator.hasNext()) {
                if (!item.equals(classifier.apply(iterator.next()))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static <X> boolean allElementsEqual(Iterable<X> xs) {
        return allElementsEqual(xs, Function.identity());
    }

    public static <X> List<X> dropRight(List<X> xs, int n) {
        return xs.subList(0, xs.size() - n);
    }

    public static <X, Y> Y reduce(Stream<X> xs, Y init, BiFunction<Y, ? super X, Y> fn) {
        return xs.reduce(init, fn, ($1, $2) -> {throw new UnsupportedOperationException("No combiner.");});
    }

    public static <X, Y> Y reduce(Collection<X> xs, Y init, BiFunction<Y, ? super X, Y> fn) {
        return reduce(xs.stream(), init, fn);
    }

    public static <X, Y> Y reduceWhile(Stream<X> xs, Y init, Predicate<? super Y> test, BiFunction<Y, ? super X, Y> fn) {
        var iter = xs.iterator();

        var acc = init;
        while (iter.hasNext()) {
            var nextAcc = fn.apply(acc, iter.next());
            if (!test.test(nextAcc))
                return acc;
            acc = nextAcc;
        }

        return acc;
    }

    public static <X, Y> Stream<T2<X, Y>> zip(Collection<X> xs, Collection<Y> ys) {
        return zip(xs.iterator(), ys.iterator());
    }

    public static <X, Y> Stream<T2<X, Y>> zip(BaseStream<X, ?> xs, BaseStream<Y, ?> ys) {
        return zip(xs.iterator(), ys.iterator());
    }

    public static <X, Y> Stream<T2<X, Y>> zip(Iterator<X> xs, Iterator<Y> ys) {
        var iter = new Iterator<T2<X, Y>>() {
            @Override
            public boolean hasNext() {
                return xs.hasNext() && ys.hasNext();
            }

            @Override
            public T2<X, Y> next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return t2(xs.next(), ys.next());
            }
        };

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, 0), false);
    }

    public static <X> Stream<List<X>> zipAll(Collection<List<X>> lists) {
        if (lists.isEmpty()) {
            return Stream.empty();
        }
        else {
            final var maxSize = lists.stream().mapToInt(List::size).max().orElseThrow();
            var iter = new Iterator<List<X>>() {
                int i = 0;

                @Override
                public boolean hasNext() {
                    return i < maxSize;
                }

                @Override
                public List<X> next() {
                    if (!hasNext())
                        throw new NoSuchElementException();

                    var xs = lists.stream().map(list -> list.get(i)).toList();
                    i++;
                    return xs;
                }
            };

            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, 0), false);
        }
    }

    public static <X, Z> Z foldl(BiFunction<? super Z, ? super X, ? extends Z> fn,
                                 Z init,
                                 Collection<X> xs)
    {
        return foldl(fn, init, xs.stream());
    }

    public static <Z, X> Z foldl(BiFunction<? super Z, ? super X, ? extends Z> fn, Z init, Stream<X> xs) {
        return xs.reduce(init, fn::apply, ($1, $2) -> { throw new UnsupportedOperationException("no combiner"); });
    }

    public static <K,V,Z> Z foldl(Function3<? super Z, ? super K, ? super V, ? extends Z> fn,
                                 Z init,
                                 Map<K, V> xs)
    {
        return foldl((acc, entry) -> fn.apply(acc, entry.getKey(), entry.getValue()),
                     init,
                     xs.entrySet());
    }


    public static <K,V,Z> Stream<Z> stream(Map<K, V> map, BiFunction<? super K, ? super V, Z> fn) {
        return map.entrySet().stream().map(entry -> fn.apply(entry.getKey(), entry.getValue()));
    }

    // public static <X, Y> Stream<T2<X, Y>> zip(Iterable<X> xs, Iterable<Y> ys) {
    //
    // }
    //
    // public static <X, Y, Z> Stream<T3<X, Y, Z>> zip3(Iterable<X> xs, Iterable<Y> ys, Iterable<Z> zs) {
    //
    // }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private Util() {};

}
