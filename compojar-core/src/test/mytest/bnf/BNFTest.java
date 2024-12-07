// package compojar.bnf;
//
// import org.junit.Test;
//
// import java.util.Set;
//
// import static compojar.bnf.Rule.rule;
//
// public class BNFTest {
//
//     @Test
//     public void as_followed_by_equal_number_of_bs() {
//         enum T implements Terminal {
//             a, b
//         }
//         enum V implements Variable {
//             S, A, B
//         }
//
//         var bnf = new BNF(
//                 Set.of(rule(V.S, T.a, V.A, V.S),
//                        rule(V.A, T.a, V.A, V.A),
//                        rule(V.A, T.b),
//                        rule(V.S, T.b, V.B, V.S),
//                        rule(V.B, T.b, V.B, V.B),
//                        rule(V.B, T.a),
//                        rule(V.S)),
//                 V.S
//         );
//
//         var generator = new Generator();
//         final var api = generator.generateApi(bnf, "as_followed_by_equal_number_of_bs");
//
//         as_followed_by_equal_number_of_bs.start()
//                 .a().b().b().a().b().a().b().b().a().a().$();
//     }
//
//     public interface as_followed_by_equal_number_of_bs {
//         static S<Object> start() {
//             return null;
//         }
//
//         interface B<K> {
//             K a();
//
//             B<B<K>> b();
//         }
//
//         interface S<K> extends $End$<K> {
//             B<S<K>> b();
//
//             A<S<K>> a();
//         }
//
//         interface A<K> {
//             K b();
//
//             A<A<K>> a();
//         }
//
//         interface $End$<K> {
//             K $();
//         }
//     }
//
//
//
// }
