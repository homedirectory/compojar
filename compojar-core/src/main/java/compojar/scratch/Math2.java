package compojar.scratch;

import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;

import java.util.Set;
import java.util.function.Function;

import static compojar.bnf.Derivation.derivation;
import static compojar.bnf.Selection.selection;
import static compojar.bnf.Symbol.terminal;
import static compojar.bnf.Symbol.variable;

/*
E -> Prod
E -> Sum
E -> NegExpr
Prod -> N * N * N
Sum -> N + N
NegExpr -> - N
N -> N1
N -> N2
N1 -> 1
N2 -> 0

AST:

E - interface Expr
Prod - record Prod (Num a, Num b, Num c) implements Expr
Sum - record Sum (Num a, Num b) implements Expr
NegExpr - record NegExpr (Expr e) implements Expr
N - interface Num
N1 - record N1 () implements Num
N2 - record N2 () implements Num

Parsers:

E - Expr
Prod - Prod
Sum - Sum
NegExpr - NegExpr
N - Num
N1 - N1
N2 - N2

Eliminate common prefix:

E -> Prod_Sum
E -> NegExpr
Prod_Sum -> N O
O -> + N
O -> * N * N
NegExpr -> - N
N -> N1
N -> N2
N1 -> 1
N2 -> 0

Parsers:

E - Expr
Prod_Sum - Prod | Sum
O - (Prod - a) | (Sum - a)
NegExpr - NegExpr
N - Num
N1 - N1
N2 - N2

Replace right-terminals:

1. S -> E
2. E -> Prod_Sum
4. E -> NegExpr
5. Prod_Sum -> N O
6. O -> + N
7. O -> * N M N
8. NegExpr -> - E
9. N -> N1
10. N -> N2
11. N1 -> 1
12. N2 -> 0
13. M -> *

The ultimate canonical form:

E -> Prod_Sum
E -> NegExpr
Prod_Sum -> N O
O -> O1
O -> O2
O1 -> + N
O2 -> * N M N
NegExpr -> - E
N -> N1
N -> N2
N1 -> 1
N2 -> 0
M -> *

Parsers:

E - Expr
Prod_Sum - Prod | Sum
O - (Prod - a) | (Sum - a)
O1 - (Sum - a)
O2 - (Prod - a)
NegExpr - NegExpr
N - Num
N1 - N1
N2 - N2

Stack machine:

| read  | pop       | push      |
| ----- | ----------| --------- |
| empty | E         | Prod_Sum  |
| empty | E         | NegExpr   |
| empty | Prod_Sum  | N O       |
| empty | O         | O1        |
| empty | O         | O2        |
| +     | O1        | N         |
| *     | O2        | N * N     |
| -     | NegExpr   | E         |
| empty | N         | N1        |
| empty | N         | N2        |
| 1     | N1        | empty     |
| 0     | N2        | empty     |
| *     | M         | empty     |

*/
public interface Math2 {

    public static void main(String[] args) {
        final var expr = start().one().plus().zero();
        final var expr1 = start().one().mul().zero().mul().zero();
        final var expr2 = start().one().mul().one().mul().one();
        final var expr3 = start().neg().one().mul().one().mul().one();
        final var expr4 = start().neg().neg().one().mul().one().mul().one();
    }

    static BNF originalBNF() {
/*
        E -> Prod
        E -> Sum
        E -> NegExpr
        Prod -> N * N * N
        Sum -> N + N
        NegExpr -> - N
        N -> N1
        N -> N2
        N1 -> 1
        N2 -> 0
*/

        Variable E = variable("E"),
                Prod = variable("Prod"),
                Sum = variable("Sum"),
                NegExpr = variable("NegExpr"),
                N = variable("N"),
                N1 = variable("N1"),
                N2 = variable("N2");
        Terminal plus = terminal("plus"),
                mul = terminal("mul"),
                neg = terminal("neg"),
                one = terminal("one"),
                zero = terminal("zero") ;

        return new BNF(Set.of(selection(E, Prod, Sum, NegExpr),
                              derivation(Prod, N, mul, N, mul, N),
                              derivation(Sum, N, plus, N),
                              derivation(NegExpr, neg, E),
                              selection(N, N1, N2),
                              derivation(N1, one),
                              derivation(N2, zero)),
                       E);
    }

    static BNF canonicalBNF() {
/*
        E -> Prod_Sum
        E -> NegExpr
        Prod_Sum -> N O
        O -> O1
        O -> O2
        O1 -> + N
        O2 -> * N M N
        NegExpr -> - E
        N -> N1
        N -> N2
        N1 -> 1
        N2 -> 0
        M -> *
*/
        Variable E = variable("E"),
                Prod_Sum = variable("Prod_Sum"),
                // Sum = variable("Sum"),
                NegExpr = variable("NegExpr"),
                // Prod = variable("Prod"),
                O = variable("O"),
                O1 = variable("O1"),
                O2 = variable("O2"),
                N = variable("N"),
                N1 = variable("N1"),
                N2 = variable("N2"),
                M = variable("M");
                        ;
        Terminal plus = terminal("plus"),
                mul = terminal("mul"),
                neg = terminal("neg"),
                one = terminal("one"),
                zero = terminal("zero") ;

        return new BNF(Set.of(selection(E, Prod_Sum, NegExpr),
                              derivation(Prod_Sum, N, O),
                              selection(O, O1, O2),
                              derivation(O1, plus, N),
                              derivation(O2, mul, N, M, N),
                              derivation(NegExpr, neg, E),
                              selection(N, N1, N2),
                              derivation(N1, one),
                              derivation(N2, zero),
                              derivation(M, mul)),
                       E);
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

    sealed interface Num extends AstNode {
        record N1() implements Num {}
        record N2() implements Num {}
    }

    // API

    @interface GrammarRule { String[] value(); }
    @interface ArtificalRule {}
    @interface Parser { Class<? extends AstNode>[] value();}

    // @Repeatable(PartialParsers.class)
    @interface PartialParser {
        Class<? extends AstNode> value();

        /** Names of AST node components required by this derivation partial parser. */
        String[] requires() default {};

        /** Types of AST node components required by this selection partial parser. */
        Class<? extends AstNode>[] requiresT() default {};
    }

    // @interface PartialParsers {
    //     PartialParser[] value();
    // }

    @interface Bridge {}

    @GrammarRule({"E -> Prod_Sum", "E -> NegExpr"})
    @Parser(Expr.class)
    interface _E<K> extends _NegExpr<K>, _Prod_Sum<K> {}

    @GrammarRule("Prod_Sum -> N O")
    // @Parser({Expr.Prod.class, Expr.Sum.class})
    @Parser(Expr.class)
    interface _Prod_Sum<K> extends _N<_O<K>> {}

    @GrammarRule({"O -> O1", "O -> O2"})
    @PartialParser(value = Expr.class, requiresT = {Num.class})
    @ArtificalRule
    @CommonPrefix({_N.class})
    interface _O<K> extends _O1<K>, _O2<K> {}

    @GrammarRule("O1 -> + N")
    @PartialParser(value = Expr.Sum.class, requires = {"a"})
    interface _O1<K> {
        _N<K> plus();
    }

    @GrammarRule("O2 -> * N * N")
    @PartialParser(value = Expr.Prod.class, requires = {"a"})
    interface _O2<K> {
        _N<_M<_N<K>>> mul();
    }

    @GrammarRule("NegExpr -> - E")
    @Parser(Expr.NegExpr.class)
    interface _NegExpr<K> {
        _E<K> neg();
    }

    @GrammarRule({"N -> N1", "N -> N2"})
    @Parser(Num.class)
    interface _N<K> extends _N1<K>, _N2<K> {}

    @GrammarRule("N1 -> 1")
    @Parser(Num.N1.class)
    interface _N1<K> {
        K one();
    }

    @GrammarRule("N2 -> 0")
    @Parser(Num.N2.class)
    interface _N2<K> {
        K zero();
    }

    @GrammarRule("M -> mul")
    @Bridge
    interface _M<K> {
        K mul();
    }

    // API Implementation

    @interface CommonPrefix { Class<?>[] value(); }

    final class _E_<K> implements _E<K>
    {
        private final Function<Expr, K> k;

        public _E_(final Function<Expr, K> k) {
            this.k = k;
        }

        @Override
        public _E<K> neg() {
            return new _NegExpr_<>(k).neg();
        }

        @Override
        public _O<K> zero() {
            return new _Prod_Sum_<>(k).zero();
        }

        @Override
        public _O<K> one() {
            return new _Prod_Sum_<>(k).one();
        }

    }

    final class _NegExpr_<K> implements _NegExpr<K>
    {
        private final Function<? super Expr.NegExpr, K> k;

        public _NegExpr_(final Function<? super Expr.NegExpr, K> k) {
            this.k = k;
        }

        @Parser(Expr.NegExpr.class)
        @Override
        public _E<K> neg() {
            return new _E_<>(expr -> k.apply(new Expr.NegExpr(expr)));
        }
    }

    final class _Prod_Sum_<K>
            extends _N_<_O<K>>
            implements _Prod_Sum<K>
    {
        public _Prod_Sum_(final Function<Expr, K> k) {
            super(a -> new _O_<>(a, k));
        }
    }

    final class _O_<K> implements _O<K>
    {
        private final Num a;
        private final Function<Expr, K> k;

        public _O_(final Num a,
                   final Function<Expr, K> k)
        {
            this.a = a;
            this.k = k;
        }

        @Parser(Expr.Sum.class)
        @Override
        public _N<K> plus() {
            return new _O1_<>(a, k).plus();
        }

        @Parser(Expr.Prod.class)
        @Override
        public _N<_M<_N<K>>> mul() {
            return new _O2_<>(a, k).mul();
        }
    }

    final class _O1_<K> implements _O1<K> {
        private final Num a;
        private final Function<? super Expr.Sum, K> k;

        public _O1_(final Num a, final Function<? super Expr.Sum, K> k) {
            this.a = a;
            this.k = k;
        }

        @Override
        public _N<K> plus() {
            return new _N_<>(b -> k.apply(new Expr.Sum(a, b)));
        }
    }

    final class _O2_<K> implements _O2<K> {
        private final Num a;
        private final Function<? super Expr.Prod, K> k;

        public _O2_(final Num a, final Function<? super Expr.Prod, K> k) {
            this.a = a;
            this.k = k;
        }

        @Override
        public _N<_M<_N<K>>> mul() {
            return new _N_<>(b -> new _M_<>(new _N_<>(c -> k.apply(new Expr.Prod(a, b, c)))));
        }
    }

    @Parser(Num.class)
    class _N_<K> implements _N<K>
    {
        private final Function<Num, K> k;

        public _N_(final Function<Num, K> k) {
            this.k = k;
        }

        @Override
        public K zero() {
            return new _N2_<>(k).zero();
        }

        @Override
        public K one() {
            return new _N1_<>(k).one();
        }
    }

    @Parser(Num.N1.class)
    final class _N1_<K> implements _N1<K>
    {
        private final Function<? super Num.N1, K> k;

        public _N1_(final Function<? super Num.N1, K> k) {
            this.k = k;
        }

        @Override
        public K one() {
            return k.apply(new Num.N1());
        }
    }

    @Parser(Num.N2.class)
    final class _N2_<K> implements _N2<K>
    {
        private final Function<? super Num.N2, K> k;

        public _N2_(final Function<? super Num.N2, K> k) {
            this.k = k;
        }

        @Override
        public K zero() {
            return k.apply(new Num.N2());
        }
    }

    final class _M_<K> implements _M<K> {
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
