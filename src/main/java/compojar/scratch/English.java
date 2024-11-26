package compojar.scratch;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 1. S -> A N V
 * 2. A -> a | the
 * 3. N -> J N | cat | rat | dog
 * 4. J -> big | small | fast
 * 5. V -> eats | escapes | chases
 */
public interface English {

    public static void main(String[] args) {
        var s1 = start().a().big().big().big().cat().chases().dog();
        var s2 = start().the().dog().eats().small().small().rat();
    }

    static _Sentence<Sentence> start() {
        return new _Sentence_<>(Function.identity());
    }

    // AST

    record Sentence (Article article, Noun noun1, Verb verb, Noun noun2) {
        @Override public String toString() {
            return Stream.of(article, noun1, verb, noun2).map(Object::toString).collect(Collectors.joining(" "));
        }
    }

    record Article(String word) {
        @Override public String toString() {
            return word;
        }
    }

    interface Noun {}
    enum FixedNoun implements Noun {
        cat, rat, dog;
    }
    record JNoun(Adjective adjective, Noun noun) implements Noun {
        @Override public String toString() {
            return adjective.toString() + " " + noun.toString();
        }
    }

    enum Adjective {
        big, small, fast;
    }

    enum Verb {
        eats, escapes, chases;
    }

    // API

    interface _Sentence<K> extends _Article<_Noun<_Verb<_Noun<K>>>> {}

    interface _Article<K> {
        K a();
        K the();
    }

    interface _Noun<K> extends _Adjective<_Noun<K>> {
        K cat();
        K rat();
        K dog();
    }

    interface _Adjective<K> {
        K big();
        K small();
        K fast();
    }

    interface _Verb<K> {
        K eats();
        K escapes();
        K chases();
    }

    // API Implementation

    class _Article_<K> implements _Article<K> {
        private final Function<Article, K> f;

        public _Article_(Function<Article, K> f) {
            this.f = f;
        }

        @Override
        public K a() {
            return f.apply(new Article("a"));
        }

        @Override
        public K the() {
            return f.apply(new Article("the"));
        }
    }

    class _Adjective_<K> implements _Adjective<K> {
        protected final Function<Adjective, K> f;

        public _Adjective_(Function<Adjective, K> f) {
            this.f = f;
        }

        @Override
        public K big() {
            return f.apply(Adjective.big);
        }

        @Override
        public K small() {
            return f.apply(Adjective.small);
        }

        @Override
        public K fast() {
            return f.apply(Adjective.fast);
        }
    }

    class _Noun_<K> extends _Adjective_<_Noun<K>> implements _Noun<K> {
        private final Function<Noun, K> f;

        public _Noun_(Function<Noun, K> f) {
            // super(adjective -> (noun1 -> new NounJ(adjective)));
            super(adjective -> new _Noun_<>(noun -> f.apply(new JNoun(adjective, noun))));
            this.f = f;
        }

        @Override
        public K cat() {
            return f.apply(FixedNoun.cat);
        }

        @Override
        public K rat() {
            return f.apply(FixedNoun.rat);
        }

        @Override
        public K dog() {
            return f.apply(FixedNoun.dog);
        }
    }

    record _Verb_<K>(Function<Verb, K> f) implements _Verb<K> {
        @Override
        public K eats() {
            return f.apply(Verb.eats);
        }

        @Override
        public K escapes() {
            return f.apply(Verb.escapes);
        }

        @Override
        public K chases() {
            return f.apply(Verb.chases);
        }
    }

    final class _Sentence_<K> extends _Article_<_Noun<_Verb<_Noun<K>>>> implements _Sentence<K> {
        public _Sentence_(Function<Sentence, K> f) {
            super(article -> new _Noun_<_Verb<_Noun<K>>>(noun1 -> new _Verb_<_Noun<K>>(verb -> new _Noun_<K>(noun2 -> f.apply(new Sentence(article, noun1, verb, noun2))))));
        }
    }

}
