//package compojar.bnf;
//
//import com.squareup.javapoet.ClassName;
//import compojar.gen.AstGenerator;
//import compojar.gen.Namer;
//import compojar.gen.ParserInfo;
//import org.junit.Test;
//
//import java.util.Map;
//import java.util.Set;
//
//import static compojar.bnf.Rule.derivation;
//import static compojar.bnf.Rule.selection;
//import static compojar.bnf.Symbol.terminal;
//import static compojar.bnf.Symbol.variable;
//import static compojar.bnf.TestUtils.assertBnfSemanticEquals;
//import static compojar.util.Util.mapOf;
//import static org.junit.Assert.assertEquals;
//
//public class LeftFactoringTest {
//
//    static final Terminal
//            x = terminal("x"), y = terminal("y"), mul = terminal("mul"), plus = terminal("plus"),
//            z = terminal("z"),
//            k = terminal("k"),
//            b = terminal("b"),
//            c = terminal("c"),
//            m = terminal("m"),
//            exp = terminal("exp"),
//            e = terminal("e"),
//            l = terminal("l"),
//            d = terminal("d"),
//            g = terminal("g"),
//            i = terminal("i"),
//            f = terminal("f"),
//            div = terminal("div"),
//            j = terminal("j")
//                    ;
//                    ;
//    static final Variable
//            A = variable("A"), B = variable("B"), C = variable("C"), F = variable("F"), G = variable("G"),
//            A_B = variable("A_B"),
//            C_F = variable("C_F"),
//            K_M = variable("K_M"),
//            C_M = variable("C_M"),
//            G0 = variable("G0"),
//            G1 = variable("G1"),
//            G0_F = variable("G0_F"),
//            G0_M = variable("G0_M"),
//            G0_K = variable("G0_K"),
//            G0_C = variable("G0_C"),
//            G0_A = variable("G0_A"),
//            G0_B = variable("G0_B"),
//            G1_F = variable("G1_F"),
//            G1_C = variable("G1_C"),
//            M = variable("M"),
//            N = variable("N"),
//            K = variable("K"),
//            X = variable("X"),
//            Z = variable("Z"),
//            Y = variable("Y"),
//            Y1 = variable("Y1"),
//            E = variable("E"),
//            D = variable("D"),
//            D1 = variable("D1"),
//            D2 = variable("D2"),
//            L = variable("L"),
//            I = variable("I"),
//            J = variable("J"),
//            OP = variable("OP")
//                    ;
//
//    final Namer namer = new Namer("LeftFactoring", "lf");
//
//    @Test
//    public void indirect_common_prefix_2_levels() {
//        var bnf = new BNF(Set.of(selection(A, B, C),
//                                 selection(B, F, G),
//                                 derivation(F, x, mul, y),
//                                 derivation(G, y),
//                                 derivation(C, x, plus, y)),
//                          A);
//        var astMetadata = new AstGenerator(namer, bnf).generate().snd();
//
//        var expectedBnf = new BNF(Set.of(selection(A, C_F, G),
//                                          derivation(C_F, x, G0),
//                                          selection(G0, G0_F, G0_C),
//                                          derivation(G0_F, mul, y),
//                                          derivation(G0_C, plus, y),
//                                          derivation(G, y)),
//                                  A);
//
//        var expectedParserInfoMap = Map.<Variable, ParserInfo>of(
//                A, new ParserInfo.Full(astNodeName("A")),
//                C_F, new ParserInfo.Full(astNodeName("A")),
//                G0, new ParserInfo.PartialS(astNodeName("A"), Set.of()),
//                G0_F, new ParserInfo.PartialD(astNodeName("F"), Set.of()),
//                G0_C, new ParserInfo.PartialD(astNodeName("C"), Set.of()),
//                G, new ParserInfo.Full(astNodeName("G")));
//        var expectedAstMetadata = astMetadata.updateParserInfos(expectedParserInfoMap);
//
//        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
//        assertBnfSemanticEquals(expectedBnf, result.bnf());
//        assertEquals(expectedAstMetadata, result.astMetadata());
//    }
//
//    @Test
//    public void indirect_common_prefix_3_levels() {
//        var bnf = new BNF(Set.of(selection(A, B, C),
//                                 selection(B, F, G),
//                                 selection(F, M, N),
//                                 derivation(M, x, mul, y),
//                                 derivation(N, z),
//                                 derivation(G, y),
//                                 derivation(C, x, plus, y)),
//                          A);
//        var astMetadata = new AstGenerator(namer, bnf).generate().snd();
//
//        var expectedBnf = new BNF(Set.of(selection(A, C_M, N, G),
//                                         derivation(C_M, x, G0),
//                                         selection(G0, G0_M, G0_C),
//                                         derivation(G0_M, mul, y),
//                                         derivation(G0_C, plus, y),
//                                         derivation(N, z),
//                                         derivation(G, y)),
//                                  A);
//
//        var expectedParserInfoMap = Map.<Variable, ParserInfo>of(
//                A, new ParserInfo.Full(astNodeName("A")),
//                C_M, new ParserInfo.Full(astNodeName("A")),
//                G0, new ParserInfo.PartialS(astNodeName("A"), Set.of()),
//                G0_M, new ParserInfo.PartialD(astNodeName("M"), Set.of()),
//                G0_C, new ParserInfo.PartialD(astNodeName("C"), Set.of()),
//                N, new ParserInfo.Full(astNodeName("N")),
//                G, new ParserInfo.Full(astNodeName("G")));
//        var expectedAstMetadata = astMetadata.updateParserInfos(expectedParserInfoMap);
//
//        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
//        assertBnfSemanticEquals(expectedBnf, result.bnf());
//        assertEquals(expectedAstMetadata, result.astMetadata());
//    }
//
//    @Test
//    public void indirect_common_prefix_3_levels_and_first_variable() {
//        var bnf = new BNF(Set.of(selection(A, B, C),
//                                 selection(B, F, G),
//                                 selection(F, M, N),
//                                 derivation(M, X, mul, y),
//                                 derivation(N, z),
//                                 derivation(G, y),
//                                 derivation(C, X, plus, y),
//                                 derivation(X, x)),
//                          A);
//        var astMetadata = new AstGenerator(namer, bnf).generate().snd();
//
//        var expectedBnf = new BNF(Set.of(selection(A, C_M, N, G),
//                                         derivation(C_M, X, G0),
//                                         selection(G0, G0_M, G0_C),
//                                         derivation(G0_M, mul, y),
//                                         derivation(G0_C, plus, y),
//                                         derivation(N, z),
//                                         derivation(G, y),
//                                         derivation(X, x)),
//                                  A);
//
//        Map<Variable, ParserInfo> expectedParserInfoMap = mapOf(
//                A, new ParserInfo.Full(astNodeName("A")),
//                C_M, new ParserInfo.Full(astNodeName("A")),
//                G0, new ParserInfo.PartialS(astNodeName("A"), Set.of(astNodeName("X"))),
//                G0_M, new ParserInfo.PartialD(astNodeName("M"), Set.of("x0")),
//                G0_C, new ParserInfo.PartialD(astNodeName("C"), Set.of("x0")),
//                N, new ParserInfo.Full(astNodeName("N")),
//                G, new ParserInfo.Full(astNodeName("G")),
//                X, new ParserInfo.Full(astNodeName("X")));
//        var expectedAstMetadata = astMetadata.updateParserInfos(expectedParserInfoMap);
//
//        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
//        assertBnfSemanticEquals(expectedBnf, result.bnf());
//        assertEquals(expectedAstMetadata, result.astMetadata());
//    }
//
//    @Test
//    public void multiple_common_prefixes() {
//        var bnf = new BNF(Set.of(selection(A, B, C),
//                                 selection(B, F, G),
//                                 derivation(F, x, mul, y),
//                                 selection(G, K, M),
//                                 derivation(K, z, k),
//                                 derivation(M, z, m),
//                                 derivation(C, x, plus, y)),
//                          A);
//        var astMetadata = new AstGenerator(namer, bnf).generate().snd();
//
//        var expectedBnf = new BNF(Set.of(selection(A, C_F, K_M),
//                                         derivation(K_M, z, G0),
//                                         selection(G0, G0_K, G0_M),
//                                         derivation(G0_K, k),
//                                         derivation(G0_M, m),
//                                         derivation(C_F, x, G1),
//                                         selection(G1, G1_F, G1_C),
//                                         derivation(G1_F, mul, y),
//                                         derivation(G1_C, plus, y)),
//                                  A);
//
//        Map<Variable, ParserInfo> expectedParserInfoMap = mapOf(
//                A, new ParserInfo.Full(astNodeName("A")),
//                C_F, new ParserInfo.Full(astNodeName("A")),
//                K_M, new ParserInfo.Full(astNodeName("A")),
//                G0, new ParserInfo.PartialS(astNodeName("A"), Set.of()),
//                G0_K, new ParserInfo.PartialD(astNodeName("K"), Set.of()),
//                G0_M, new ParserInfo.PartialD(astNodeName("M"), Set.of()),
//                G1, new ParserInfo.PartialS(astNodeName("A"), Set.of()),
//                G1_F, new ParserInfo.PartialD(astNodeName("F"), Set.of()),
//                G1_C, new ParserInfo.PartialD(astNodeName("C"), Set.of())
//        );
//        var expectedAstMetadata = astMetadata.updateParserInfos(expectedParserInfoMap);
//
//        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
//        assertBnfSemanticEquals(expectedBnf, result.bnf());
//        assertEquals(expectedAstMetadata, result.astMetadata());
//    }
//
//    ClassName astNodeName(CharSequence name) {
//        return namer.astNodeClassName(name.toString());
//    }
//
//    @Test
//    public void chain_of_length_1() {
//        // TODO update me
////        var bnf = new BNF(Set.of(selection(A, B, C),
////                                 derivation(C, x),
////                                 derivation(B, C, y)),
////                          A);
////
////        var astMetadata = new AstGenerator(namer, bnf).generate().snd();
////
////        var expectedBnf = new BNF(Set.of(selection(A, A_B),
////                                         derivation(A_B, C, G0),
////                                         selection(G0, G0_A, G0_B),
////                                         derivation(G0_A),
////                                         derivation(G0_B, y),
////                                         derivation(C, x)),
////                          A);
////
////        Map<Variable, ParserInfo> expectedParserInfoMap = mapOf(
////                A, new ParserInfo.Full(astNodeName("A")),
////                A_B, new ParserInfo.Full(astNodeName("A")),
////                G0, new ParserInfo.PartialS(astNodeName("A"), Set.of(astNodeName("C"))),
////                G0_A, new ParserInfo.PartialEmpty(astNodeName("C")),
////                G0_B, new ParserInfo.PartialD(astNodeName("B"), Set.of("c0")),
////                C, new ParserInfo.Full(astNodeName("C"))
////        );
////        var expectedAstMetadata = astMetadata.updateParserInfos(expectedParserInfoMap);
////
////        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
////        assertBnfSemanticEquals(expectedBnf, result.bnf());
////        assertEquals(expectedAstMetadata, result.astMetadata());
//    }
//
//    @Test
//    public void crazy_grammar_with_all_sorts_of_rules_where_common_prefix_is_a_terminal() {
//        // A -> B | G | Y | K   # Full A
//        // B -> E | F | I | X   # Full B
//        // E -> L + e           # Full E
//        // L -> x ^ l           # Full L
//        // G -> D * g           # Full G
//        // D -> D1 | D2         # Full D
//        // D1 -> x d            # Full D1
//        // D2 -> d              # Full D2
//        // Y -> x / y           # Full Y
//        // F -> f               # Full F
//        // I -> x i             # Full I
//        // K -> k               # Full K
//        // X -> x               # Full X
//
//        var bnf = new BNF(Set.of(selection(A, B, G, Y, K),
//                                 selection(B, E, F, I, X),
//                                 derivation(E, L, plus, e),
//                                 derivation(L, x, exp, l),
//                                 derivation(G, D, mul, g),
//                                 selection(D, D1, D2),
//                                 derivation(D1, x, d),
//                                 derivation(D2, d),
//                                 derivation(Y, x, div, y),
//                                 derivation(F, f),
//                                 derivation(I, x, i),
//                                 derivation(K, k),
//                                 derivation(X, x)),
//                          A);
//
//        var astMetadata = new AstGenerator(namer, bnf).generate().snd();
//
//        // The expected BNF is:
//        // A -> O | K | B | G  # Full A
//        //
//        // O -> x _O           # Full A
//        // _O -> _B | _G | _Y  # PartialS A ()
//        //
//        // B -> F              # Full B
//        // _B -> _E | _I | _X  # PartialS B ()
//        // _E -> _L + e        # PartialD E ()
//        // _L -> ^ l           # PartialD L ()
//        // G -> D * g          # Full G
//        // _G -> _D * g        # PartialD G ()
//        // D -> D2             # Full D
//        // D2 -> d          # Full D2
//        // _D -> _D1          # PartialS D ()
//        // _D1 -> d          # PartialD D1 ()
//        // _Y -> / y          # PartialD Y ()
//        // _I -> i          # Partial I
//        // _X -> empty      # Partial X
//        //
//        // K -> k
//
//        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
//    }
//
//    @Test
//    public void crazy_grammar_with_all_sorts_of_rules_where_common_prefix_is_a_non_terminal() {
//        // A -> B | G | Y | K   # Full A
//        // B -> E | F | I | X   # Full B
//        // E -> L + e           # Full E
//        // L -> X ^ l           # Full L
//        // G -> D * g           # Full G
//        // D -> D1 | D2         # Full D
//        // D1 -> X              # Full D1
//        // D2 -> d              # Full D2
//        // Y -> X / y           # Full Y
//        // F -> f               # Full F
//        // I -> X | J           # Full I
//        // J -> j               # Full J
//        // K -> k               # Full K
//        // X -> x               # Full X
//
//        var bnf = new BNF(Set.of(selection(A, B, G, Y, K),
//                                 selection(B, E, F, I, X),
//                                 derivation(E, L, plus, e),
//                                 derivation(L, X, exp, l),
//                                 derivation(G, D, mul, g),
//                                 selection(D, D1, D2),
//                                 derivation(D1, X),
//                                 derivation(D2, d),
//                                 derivation(Y, X, div, y),
//                                 derivation(F, f),
//                                 selection(I, X, J),
//                                 derivation(J, j),
//                                 derivation(K, k),
//                                 derivation(X, x)),
//                          A);
//
//        var astMetadata = new AstGenerator(namer, bnf).generate().snd();
//
//        // The expected BNF is:
//
//        // A -> O | K | B | G  # Full A
//        //
//        // O -> x _O           # Full A
//        // _O -> _B | _G | _Y  # PartialS A ()
//        //
//        // B -> F              # Full B
//        // _B -> _E | _I | _X  # PartialS B ()
//        // _E -> _L + e        # PartialD E ()
//        // _L -> _X ^ l           # PartialD L
//        // G -> D * g          # Full G
//        // _G -> _D * g        # PartialD G ()
//        // D -> D2          # Full D
//        // D2 -> d          # Full D2
//        // _D -> _D1          # PartialS D ()
//        // _D1 -> _X         # PartialD D1 ()
//        // _Y -> _X / y          # PartialD _Y  ()
//        // I -> J             # Full I
//        // _I -> _X         # PartialS I ()
//        // _X -> empty      # Partial Empty X ()
//        //
//        // K -> k           # Full K
//
//        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
//    }
//
//    @Test
//    public void embedded_common_prefix() {
//        // A -> B | C
//        // B -> x + y
//        // C -> x + z
//
//        // var bnf = new BNF(Set.of(selection(A, B, C, D),
//        //                          derivation(B, x, plus, y),
//        //                          derivation(C, x, plus, z),
//        //                          derivation(D, x, plus)),
//        //                   A);
//        // var bnf = new BNF(Set.of(selection(A, B, C),
//        //                          derivation(B, D, y),
//        //                          derivation(C, D, z),
//        //                          derivation(D, x, plus)),
//        //                   A);
//        // var bnf = new BNF(Set.of(selection(A, B, C, E),
//        //                          derivation(B, D, y),
//        //                          derivation(C, D, z),
//        //                          derivation(D, x, OP),
//        //                          derivation(E, x, OP, e),
//        //                          derivation(OP, plus)),
//        //                   A);
//        // var bnf = new BNF(Set.of(selection(A, B, C, E),
//        //                          derivation(B, D, y),
//        //                          derivation(C, D, z),
//        //                          derivation(D, OP, x),
//        //                          derivation(E, OP, x, e),
//        //                          derivation(OP, plus)),
//        //                   A);
//         var bnf = new BNF(Set.of(selection(A, B, C),
//                                  derivation(B, D, y),
//                                  derivation(C, D, z),
//                                  derivation(D, OP),
//                                  derivation(OP, plus)),
//                           A);
////        var bnf = new BNF(Set.of(selection(A, B, C),
////                                 derivation(B, Z, b),
////                                 derivation(C, Z, c),
////                                 selection(Z, X, Y, Y1),
////                                 derivation(X, x),
////                                 derivation(Y, y),
////                                 derivation(Y1, y, k)),
////                          A);
//        var astMetadata = new AstGenerator(namer, bnf).generate().snd();
//
//        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
//    }
//
//}
