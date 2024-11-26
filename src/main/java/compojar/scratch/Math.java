package compojar.scratch;

import java.util.function.Function;

/*
1. S -> E
2. E -> Prod
3. E -> N + N          # Sum
4. E -> - E             # NegExpr
5. Prod -> N * N * N    # Prod
6. N -> 1              # Num
7. N -> 0

After transformations:

1. S -> E
2. E -> N O
3. O -> P N
4. O -> M N M N
5. E -> - N
6. N -> 1
7. N -> 0
8. P -> +
9. M -> *

Stack machine:

rule | read  | pop  | push      |
---- | ----- | ---- | --------- |
1    | empty | S    | E         |
2    | empty | E    | NO        |
3    | empty | O    | PN        |
4    | empty | O    | MNMN      |
5    | -     | E    | E         |
6    | 1     | N    | empty     |
7    | 0     | N    | empty     |
8    | +     | P    | empty     |
9    | *     | M    | empty     |

---

1. S -> E
2. E -> Prod
3. E -> Sum
4. E -> NegExpr
5. Prod -> N * N * N    # Prod
6. Sum -> N + N
6. N -> 1              # Num
7. N -> 0

*/
public interface Math {

    public static void main(String[] args) {
        final var expr = start().one().plus().zero();
        final var expr1 = start().one().mul().zero().mul().zero();
        final var expr2 = start().one().mul().one().mul().one();
        final var expr3 = start().neg().one().mul().one().mul().one();
        final var expr4 = start().neg().neg().one().mul().one().mul().one();
    }

    static _E<Expr> start() {
        return new _E_<>(Function.identity());
    }

    // AST

    sealed interface AstNode {}

    sealed interface Expr extends AstNode {
        record NegExpr (Expr expr) implements Expr {}
        record Prod (Num a, Num b, Num c) implements Expr {}
        record Sum (Num a, Num b) implements Expr {}
    }

    enum Num implements AstNode {one, zero}

    // API

    @interface RuleModel { String[] value(); }
    @interface ArtificalRule {}
    @interface Terminal { String value(); }

    @RuleModel({"E -> N O", "E -> - E"})
    @Parser(Expr.class)
    interface _E<K> extends _N<_O<K>> {
        _E<K> neg();
    }

    @RuleModel({"O -> P N", "O -> M N M N"})
    @Parser({Expr.Sum.class, Expr.Prod.class})
    @ArtificalRule
    @CommonPrefix({_N.class})
    interface _O<K> extends _P<_N<K>>, _M<_N<_M<_N<K>>>> {}

    @RuleModel({"N -> zero", "N -> one"})
    @Parser(Num.class)
    interface _N<K> {
        K zero();
        K one();
    }

    @RuleModel("P -> plus")
    @Terminal("plus")
    interface _P<K> {
        K plus();
    }

    @RuleModel("M -> mul")
    @Terminal("mul")
    interface _M<K> {
        K mul();
    }

    // API Implementation

    @interface Parser {
        Class<? extends AstNode>[] value();

        boolean aux() default false;
    }

    @interface CommonPrefix { Class<?>[] value(); }

    @Parser(Expr.class)
    class _E_<K>
            extends _N_<_O<K>>
            implements _E<K>
    {

        private final Function<Expr, K> k;

        public _E_(final Function<Expr, K> k) {
            super(a -> new _O_<>(k, a));
            this.k = k;
        }

        @Parser(Expr.NegExpr.class)
        @Override
        public _E<K> neg() {
            return new _E_<>(expr -> k.apply(new Expr.NegExpr(expr)));
        }

    }

    @Parser(Num.class)
    class _N_<K> implements _N<K>
    {
        private final Function<Num, K> f;

        public _N_(final Function<Num, K> f) {
            this.f = f;
        }

        @Override
        public K zero() {
            return f.apply(Num.zero);
        }

        @Override
        public K one() {
            return f.apply(Num.one);
        }
    }

    @Parser(value = Expr.class, aux = true)
    class _O_<K> implements _O<K>
    // extends _P_<_N<K>>, _M_<_N<_M<_N<K>>>>
    {
        private final Function<Expr, K> k;
        private final Num a;

        public _O_(final Function<Expr, K> k,
                   final Num a)
        {
            this.k = k;
            this.a = a;
        }

        @Parser(Expr.Sum.class)
        @Override
        public _N<K> plus() {
            return new _N_<>(b -> k.apply(new Expr.Sum(a, b)));
        }

        @Parser(Expr.Prod.class)
        @Override
        public _N<_M<_N<K>>> mul() {
            return new _N_<>(b -> new _M_<>(new _N_<>(c -> k.apply(new Expr.Prod(a, b, c)))));
        }
    }

    class _P_<K> implements _P<K> {
        private final K k;

        public _P_(final K k) {
            this.k = k;
        }

        @Override
        public K plus() {
            return k;
        }
    }

    class _M_<K> implements _M<K> {
        private final K k; // or Supplier<K>

        public _M_(final K k) {
            this.k = k;
        }

        @Override
        public K mul() {
            return k;
        }
    }

}
