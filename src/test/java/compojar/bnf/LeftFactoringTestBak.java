// package compojar.bnf;
//
// import com.squareup.javapoet.ClassName;
// import compojar.gen.Namer;
// import compojar.gen.ParserInfo;
// import org.junit.Test;
//
// import java.util.Map;
// import java.util.Set;
//
// import static compojar.bnf.Rule.derivation;
// import static compojar.bnf.Rule.selection;
// import static compojar.bnf.Symbol.terminal;
// import static compojar.bnf.Symbol.variable;
// import static compojar.util.Util.mapOf;
// import static org.junit.Assert.assertEquals;
//
// public class LeftFactoringTestBak {
//
//     @Test
//     public void indirect_common_prefix_2_levels() {
//         interface $ {
//             static ClassName astNodeName(CharSequence name) {
//                 return ClassName.get("", name.toString());
//             }
//         }
//
//         var namer = new Namer("LeftFactoring", "lf");
//
//         Terminal x = terminal("x"), y = terminal("y"), mul = terminal("mul"), plus = terminal("plus");
//         Variable A = variable("A"), B = variable("B"), C = variable("C"), F = variable("F"), G = variable("G"),
//                 F_C = variable("F_C"), G0 = variable("G0"), G0_F = variable("G0_F"), G0_C = variable("G0_C"),
//                 MUL = variable("MUL"), PLUS = variable("PLUS"), Y = variable("Y");
//
//         var bnf = new BNF(Set.of(selection(A, B, C),
//                                  selection(B, F, G),
//                                  derivation(F, x, mul, y),
//                                  derivation(G, y),
//                                  derivation(C, x, plus, y)),
//                           A);
//         var parserInfoMap = Map.<Variable, ParserInfo>of(A, new ParserInfo.Full($.astNodeName("A")),
//                                    B, new ParserInfo.Full($.astNodeName("B")),
//                                    F, new ParserInfo.Full($.astNodeName("F")),
//                                    G, new ParserInfo.Full($.astNodeName("G")),
//                                    C, new ParserInfo.Full($.astNodeName("C")));
//
//         var expectedBnf = new BNF(Set.of(selection(A, F_C, G),
//                                           derivation(F_C, x, G0),
//                                           selection(G0, G0_F, G0_C),
//                                           derivation(G0_F, MUL, Y),
//                                           derivation(G0_C, PLUS, Y),
//                                           derivation(MUL, mul),
//                                           derivation(PLUS, plus),
//                                           derivation(Y, y),
//                                           derivation(G, y)),
//                                   A);
//
//         var expectedParserInfoMap = Map.of(A, new ParserInfo.Full($.astNodeName("A")),
//                                            F_C, new ParserInfo.Full($.astNodeName("A")),
//                                            G0, new ParserInfo.PartialS($.astNodeName("A"), Set.of()),
//                                            G0_F, new ParserInfo.PartialD($.astNodeName("F"), Set.of()),
//                                            G0_C, new ParserInfo.PartialD($.astNodeName("C"), Set.of()),
//                                            MUL, ParserInfo.BRIDGE,
//                                            PLUS, ParserInfo.BRIDGE,
//                                            Y, ParserInfo.BRIDGE,
//                                            G, new ParserInfo.Full($.astNodeName("G")));
//
//         var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, parserInfoMap, AstMetadata.EMPTY));
//         assertEquals(expectedBnf, result.bnf());
//         assertEquals(expectedParserInfoMap, result.parserInfoMap());
//     }
//
//     @Test
//     public void indirect_common_prefix_3_levels() {
//         interface $ {
//             static ClassName astNodeName(CharSequence name) {
//                 return ClassName.get("", name.toString());
//             }
//         }
//
//         var namer = new Namer("LeftFactoring", "lf");
//
//         Terminal x = terminal("x"), y = terminal("y"), mul = terminal("mul"), plus = terminal("plus"), z = terminal("z");
//         Variable A = variable("A"), B = variable("B"), C = variable("C"), F = variable("F"), G = variable("G"),
//                 M_C = variable("M_C"), G0 = variable("G0"), G0_M = variable("G0_M"), G0_C = variable("G0_C"),
//                 M = variable("M"), N = variable("N"),
//                 MUL = variable("MUL"), PLUS = variable("PLUS"), Y = variable("Y");
//
//         var bnf = new BNF(Set.of(selection(A, B, C),
//                                  selection(B, F, G),
//                                  selection(F, M, N),
//                                  derivation(M, x, mul, y),
//                                  derivation(N, z),
//                                  derivation(G, y),
//                                  derivation(C, x, plus, y)),
//                           A);
//         var parserInfoMap = Map.<Variable, ParserInfo>of(A, new ParserInfo.Full($.astNodeName("A")),
//                                                          B, new ParserInfo.Full($.astNodeName("B")),
//                                                          F, new ParserInfo.Full($.astNodeName("F")),
//                                                          N, new ParserInfo.Full($.astNodeName("N")),
//                                                          M, new ParserInfo.Full($.astNodeName("M")),
//                                                          G, new ParserInfo.Full($.astNodeName("G")),
//                                                          C, new ParserInfo.Full($.astNodeName("C")));
//
//         var expectedBnf = new BNF(Set.of(selection(A, M_C, N, G),
//                                          derivation(M_C, x, G0),
//                                          selection(G0, G0_M, G0_C),
//                                          derivation(G0_M, MUL, Y),
//                                          derivation(G0_C, PLUS, Y),
//                                          derivation(N, z),
//                                          derivation(MUL, mul),
//                                          derivation(PLUS, plus),
//                                          derivation(Y, y),
//                                          derivation(G, y)),
//                                   A);
//
//         var expectedParserInfoMap = Map.of(A, new ParserInfo.Full($.astNodeName("A")),
//                                            M_C, new ParserInfo.Full($.astNodeName("A")),
//                                            G0, new ParserInfo.PartialS($.astNodeName("A"), Set.of()),
//                                            G0_M, new ParserInfo.PartialD($.astNodeName("M"), Set.of()),
//                                            G0_C, new ParserInfo.PartialD($.astNodeName("C"), Set.of()),
//                                            N, new ParserInfo.Full($.astNodeName("N")),
//                                            MUL, ParserInfo.BRIDGE,
//                                            PLUS, ParserInfo.BRIDGE,
//                                            Y, ParserInfo.BRIDGE,
//                                            G, new ParserInfo.Full($.astNodeName("G")));
//
//         var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, parserInfoMap, AstMetadata.EMPTY));
//         assertEquals(expectedBnf, result.bnf());
//         assertEquals(expectedParserInfoMap, result.parserInfoMap());
//     }
//
//     @Test
//     public void indirect_common_prefix_3_levels_and_first_variable() {
//         interface $ {
//             static ClassName astNodeName(CharSequence name) {
//                 return ClassName.get("", name.toString());
//             }
//         }
//
//         var namer = new Namer("LeftFactoring", "lf");
//
//         Terminal x = terminal("x"), y = terminal("y"), mul = terminal("mul"), plus = terminal("plus"), z = terminal("z");
//         Variable A = variable("A"), B = variable("B"), C = variable("C"), F = variable("F"), G = variable("G"),
//                 M_C = variable("M_C"), G0 = variable("G0"), G0_M = variable("G0_M"), G0_C = variable("G0_C"),
//                 M = variable("M"), N = variable("N"),
//                 MUL = variable("MUL"), PLUS = variable("PLUS"), Y = variable("Y"),
//                 X = variable("X");
//
//         var bnf = new BNF(Set.of(selection(A, B, C),
//                                  selection(B, F, G),
//                                  selection(F, M, N),
//                                  derivation(M, X, mul, y),
//                                  derivation(N, z),
//                                  derivation(G, y),
//                                  derivation(C, X, plus, y),
//                                  derivation(X, x)),
//                           A);
//         var parserInfoMap = Map.<Variable, ParserInfo>of(A, new ParserInfo.Full($.astNodeName("A")),
//                                                          B, new ParserInfo.Full($.astNodeName("B")),
//                                                          F, new ParserInfo.Full($.astNodeName("F")),
//                                                          N, new ParserInfo.Full($.astNodeName("N")),
//                                                          M, new ParserInfo.Full($.astNodeName("M")),
//                                                          G, new ParserInfo.Full($.astNodeName("G")),
//                                                          C, new ParserInfo.Full($.astNodeName("C")),
//                                                          X, new ParserInfo.Full($.astNodeName("X")));
//
//         var expectedBnf = new BNF(Set.of(selection(A, M_C, N, G),
//                                          derivation(M_C, X, G0),
//                                          selection(G0, G0_M, G0_C),
//                                          derivation(G0_M, MUL, Y),
//                                          derivation(G0_C, PLUS, Y),
//                                          derivation(N, z),
//                                          derivation(MUL, mul),
//                                          derivation(PLUS, plus),
//                                          derivation(Y, y),
//                                          derivation(G, y),
//                                          derivation(X, x)),
//                                   A);
//
//         var expectedParserInfoMap = mapOf(A, new ParserInfo.Full($.astNodeName("A")),
//                                           M_C, new ParserInfo.Full($.astNodeName("A")),
//                                           G0, new ParserInfo.PartialS($.astNodeName("A"), Set.of($.astNodeName("X"))),
//                                           G0_M, new ParserInfo.PartialD($.astNodeName("M"), Set.of("x0")),
//                                           G0_C, new ParserInfo.PartialD($.astNodeName("C"), Set.of("x0")),
//                                           N, new ParserInfo.Full($.astNodeName("N")),
//                                           MUL, ParserInfo.BRIDGE,
//                                           PLUS, ParserInfo.BRIDGE,
//                                           Y, ParserInfo.BRIDGE,
//                                           G, new ParserInfo.Full($.astNodeName("G")),
//                                           X, new ParserInfo.Full($.astNodeName("X")));
//
//         var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, parserInfoMap, AstMetadata.EMPTY));
//         assertEquals(expectedBnf, result.bnf());
//         assertEquals(expectedParserInfoMap, result.parserInfoMap());
//     }
//
// }
