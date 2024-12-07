package compojar.scratch;

import java.util.function.Function;

/**
 * Left recursive grammar.
 * <pre>
 * Expr -> Sum | Num
 * Sum -> Expr + Expr
 * Num -> one
 * </pre>
 * After eliminating left recursion:
 * <pre>
 * Expr -> Sum | Num
 * Sum -> Num + Expr Sum'
 * Sum' -> Sum'1 | Sum'2
 * Sum'1 -> + Expr Sum'
 * Sum'2 -> empty
 * Num -> one
 * </pre>
 * After eliminating common prefix Num from chains Expr -> Num & Expr -> Sum -> Num:
 * <pre>
 * Expr -> J
 * J -> Num O
 * O -> O_Expr | O_Sum
 * O_Expr -> empty
 * O_Sum -> + Expr Sum'
 * Sum' -> Sum'1 | Sum'2
 * Sum'1 -> + Expr Sum'
 * Sum'2 -> empty
 * Num -> one
 * </pre>
 * Eliminate empty productions:
 */
public interface LeftRec {

    static void main(String[] args) {
        Api.start().one().plus().one().$().$();
        Api.start().one().plus().one().plus().one().plus().one().$().$().$().$();
    }

    // AST

    interface Expr {}
    record Sum (Expr a, Expr b) implements Expr {}
    record Num () implements Expr {}

    // API

    interface Api {
        static Expr<Object> start() {
            return null;
        }

        interface Expr<K> extends J<K> {}
        interface J<K> extends Num<O<K>> {}
        interface O<K> extends O_Expr<K>, O_Sum<K> {}
        interface O_Expr<K> {
            K $();
        }
        interface O_Sum<K> {
            Expr<Sum_<K>> plus();
        }
        interface Sum_<K> extends Sum_1<K>, Sum_2<K> {}
        interface Sum_1<K> {
            Expr<Sum_<K>> plus();
        }
        interface Sum_2<K> {
            K $();
        }
        interface Num<K> {
            K one();
        }
    }

    // Full parser of Expr
    class Expr_Impl<K> implements Api.Expr<K> {
        @Override
        public Api.O<K> one() {
            return new J_Impl<K>().one();
        }
    }

    class J_Impl<K> implements Api.J<K> {

        @Override
        public Api.O<K> one() {
            return null;
        }

    }

    // Full parsers of Num
    class Num_Impl<K> implements Api.Num<K> {
        final Function<? super Num, K> k;

        public Num_Impl(final Function<? super Num, K> k) {
            this.k = k;
        }

        @Override
        public K one() {
            return k.apply(new Num());
        }
    }

}
