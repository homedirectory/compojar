package compojar.stack;

import compojar.gen.ApiGenerator;
import compojar.gen.Namer;
import org.junit.Test;

import java.util.Set;

import static compojar.stack.Rule.rule;
import static compojar.stack.Symbol.empty;
import static compojar.stack.Symbol.symbol;

public class ApiFromStackMachineTest {

    @Test
    public void math_without_left_recursion() {
        /*
1. S -> E
2. E -> Exp
3. E -> N + N
4. Exp -> N exp N
5. N -> 1
6. N -> 0

After eliminating right terminals:

3. E -> N P N
4. Exp -> N X N
7. P -> +
8. X -> exp

rule | read  | pop  | push      |
---- | ----- | ---- | --------- |
1    | empty | S    | E         |
2    | empty | E    | Exp       |
3    | empty | E    | NPN       |
4    | empty | Exp  | NXN       |
5    | 1     | N    | empty     |
6    | 0     | N    | empty     |
7    | +     | P    | empty     |
8    | exp   | X    | empty     |
         */

        Symbol S = symbol("S"), E = symbol("E"), Exp = symbol("Exp"), N = symbol("N"), P = symbol("P"), X = symbol("X"), O = symbol("O");
        Symbol exp = symbol("exp"), plus = symbol("plus"), one = symbol("one"), zero = symbol("zero");

        // var sm = new StackMachine(Set.of(rule(empty, S, E),
        //                                  rule(empty, E, Exp),
        //                                  rule(empty, E, N, P, N),
        //                                  rule(empty, Exp, N, X, N),
        //                                  rule(one, N, empty),
        //                                  rule(zero, N, empty),
        //                                  rule(plus, P, empty),
        //                                  rule(exp, X, empty)),
        //                           S);
        var sm = new StackMachine(Set.of(rule(empty, S, E),
                                         rule(empty, E, N, O),
                                         rule(empty, O, P, N),
                                         rule(empty, O, X, N),
                                         rule(one, N, empty),
                                         rule(zero, N, empty),
                                         rule(plus, P, empty),
                                         rule(exp, X, empty)),
                                  S);
        var namer = new Namer("Math", "math");
        var g = new ApiGenerator(namer, sm);
        final var api = g.generateCode();

        Math.start().one().exp().zero();
        Math.start().zero().exp().zero();
        Math.start().one().plus().one();
        Math.start().one().plus().zero();
    }

    interface Math {
        static S<Object> start() {
            return null;
        }

        interface P<K> {
            K plus();
        }

        interface S<K> extends E<K> {
        }

        interface E<K> extends N<O<K>> {
        }

        interface X<K> {
            K exp();
        }

        interface N<K> {
            K one();

            K zero();
        }

        interface O<K> extends X<N<K>>, P<N<K>> {
        }
    }



    @Test
    public void reversed_halves() {
        /*
CFG:
* S -> aSa
* S -> bSb
* S -> empty

Stack machine:

| read  | pop  | push      |
| ----- | ---- | --------- |
| a     | S    | SA        |
| a     | A    | empty     |
| b     | S    | SB        |
| b     | B    | empty     |
| empty | S    | empty     |
         */

        enum A implements Symbol {
            a, b
        }
        enum V implements Symbol {
            S, A, B
        }

        var sm = new StackMachine(Set.of(
                rule(A.a, V.S, V.S, V.A),
                rule(A.a, V.A),
                rule(A.b, V.S, V.S, V.B),
                rule(A.b, V.B),
                rule(empty, V.S)),
                                  V.S);
        var namer = new Namer("ReversedHalves", "a");
        var g = new ApiGenerator(namer, sm);
        var api = g.generateCode();

        // ReversedHalves.start().a().b().a().$().a().b().a();
    }

    interface ReversedHalves {
        static S<Object> start() {
            return null;
        }

        interface A<K> {
            K a();
        }

        interface S<K> {
            K $();

            S<A<K>> a();

            S<B<K>> b();
        }

        interface B<K> {
            K b();
        }
    }



}
