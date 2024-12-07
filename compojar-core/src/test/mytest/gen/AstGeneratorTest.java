// package compojar.gen;
//
// import compojar.bnf.BNF;
// import compojar.bnf.Terminal;
// import compojar.bnf.Variable;
// import org.junit.Test;
//
// import java.util.Set;
//
// import static compojar.bnf.Rule.rule;
// import static compojar.bnf.Symbol.terminal;
// import static compojar.bnf.Symbol.variable;
//
// public class AstGeneratorTest {
//
//     @Test
//     public void math() {
//         /*
//         2. E -> Prod
//         3. E -> N + N          # Sum
//         4. E -> - E             # NegExpr
//         5. Prod -> N * N * N    # Prod
//         6. N -> 1              # Num
//         7. N -> 0
//         */
//         Variable E = variable("E"),
//                  Prod = variable("Prod"),
//                  N = variable("N");
//         Terminal plus = terminal("plus"),
//                  mul = terminal("mul"),
//                  neg = terminal("neg"),
//                  one = terminal("one"),
//                  zero = terminal("zero") ;
//
//         var bnf = new BNF(
//                 Set.of(rule(E, Prod),
//                        rule(E, N, plus, N),
//                        rule(E, neg, E),
//                        rule(Prod, N, mul, N, mul, N),
//                        rule(N, one),
//                        rule(N, zero)),
//                 E
//         );
//
//         var g = new AstGenerator();
//         var code = g.generateCode("Math", bnf);
//         System.out.println(code);
//     }
//
//     public interface Math_AstNode {
//         class PROD implements Math_AstNode {
//             public final N n0;
//
//             public final N n1;
//
//             public final N n2;
//
//             PROD(final N n0, final N n1, final N n2) {
//                 this.n0 = n0;
//                 this.n1 = n1;
//                 this.n2 = n2;
//             }
//         }
//
//         interface E extends Math_AstNode {
//             class E0 implements E {
//                 public final N n0;
//
//                 public final N n1;
//
//                 E0(final N n0, final N n1) {
//                     this.n0 = n0;
//                     this.n1 = n1;
//                 }
//             }
//
//             class E1 implements E {
//                 public final E e0;
//
//                 E1(final E e0) {
//                     this.e0 = e0;
//                 }
//             }
//
//             class E2 implements E {
//                 public final PROD prod0;
//
//                 E2(final PROD prod0) {
//                     this.prod0 = prod0;
//                 }
//             }
//         }
//
//         interface N extends Math_AstNode {
//             class N0 implements N {
//                 N0() {
//                 }
//             }
//
//             class N1 implements N {
//                 N1() {
//                 }
//             }
//         }
//     }
//
//
// }
