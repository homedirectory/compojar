package compojar.bnf;

import com.squareup.javapoet.ClassName;
import compojar.gen.AstGenerator;
import compojar.gen.Namer;
import compojar.gen.ParserInfo;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static compojar.bnf.Rule.derivation;
import static compojar.bnf.Rule.selection;
import static compojar.bnf.Symbol.terminal;
import static compojar.bnf.Symbol.variable;
import static compojar.bnf.TestUtils.assertBnfSemanticEquals;
import static compojar.util.Util.mapOf;
import static org.junit.Assert.assertEquals;

public class LeftFactoringTest {

    static final Terminal
            x = terminal("x"), y = terminal("y"), mul = terminal("mul"), plus = terminal("plus"),
            z = terminal("z"),
            k = terminal("k"),
            m = terminal("m")
                    ;
    static final Variable
            A = variable("A"), B = variable("B"), C = variable("C"), F = variable("F"), G = variable("G"),
            A_B = variable("A_B"),
            C_F = variable("C_F"),
            K_M = variable("K_M"),
            C_M = variable("C_M"),
            G0 = variable("G0"),
            G1 = variable("G1"),
            G0_F = variable("G0_F"),
            G0_M = variable("G0_M"),
            G0_K = variable("G0_K"),
            G0_C = variable("G0_C"),
            G0_A = variable("G0_A"),
            G0_B = variable("G0_B"),
            G1_F = variable("G1_F"),
            G1_C = variable("G1_C"),
            M = variable("M"),
            N = variable("N"),
            K = variable("K"),
            X = variable("X");

    final Namer namer = new Namer("LeftFactoring", "lf");

    @Test
    public void indirect_common_prefix_2_levels() {
        var bnf = new BNF(Set.of(selection(A, B, C),
                                 selection(B, F, G),
                                 derivation(F, x, mul, y),
                                 derivation(G, y),
                                 derivation(C, x, plus, y)),
                          A);
        var astMetadata = new AstGenerator(namer, bnf).generate().snd();

        var expectedBnf = new BNF(Set.of(selection(A, C_F, G),
                                          derivation(C_F, x, G0),
                                          selection(G0, G0_F, G0_C),
                                          derivation(G0_F, mul, y),
                                          derivation(G0_C, plus, y),
                                          derivation(G, y)),
                                  A);

        var expectedParserInfoMap = Map.<Variable, ParserInfo>of(
                A, new ParserInfo.Full(astNodeName("A")),
                C_F, new ParserInfo.Full(astNodeName("A")),
                G0, new ParserInfo.PartialS(astNodeName("A"), Set.of()),
                G0_F, new ParserInfo.PartialD(astNodeName("F"), Set.of()),
                G0_C, new ParserInfo.PartialD(astNodeName("C"), Set.of()),
                G, new ParserInfo.Full(astNodeName("G")));
        var expectedAstMetadata = astMetadata.updateParserInfos(expectedParserInfoMap);

        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
        assertBnfSemanticEquals(expectedBnf, result.bnf());
        assertEquals(expectedAstMetadata, result.astMetadata());
    }

    @Test
    public void indirect_common_prefix_3_levels() {
        var bnf = new BNF(Set.of(selection(A, B, C),
                                 selection(B, F, G),
                                 selection(F, M, N),
                                 derivation(M, x, mul, y),
                                 derivation(N, z),
                                 derivation(G, y),
                                 derivation(C, x, plus, y)),
                          A);
        var astMetadata = new AstGenerator(namer, bnf).generate().snd();

        var expectedBnf = new BNF(Set.of(selection(A, C_M, N, G),
                                         derivation(C_M, x, G0),
                                         selection(G0, G0_M, G0_C),
                                         derivation(G0_M, mul, y),
                                         derivation(G0_C, plus, y),
                                         derivation(N, z),
                                         derivation(G, y)),
                                  A);

        var expectedParserInfoMap = Map.<Variable, ParserInfo>of(
                A, new ParserInfo.Full(astNodeName("A")),
                C_M, new ParserInfo.Full(astNodeName("A")),
                G0, new ParserInfo.PartialS(astNodeName("A"), Set.of()),
                G0_M, new ParserInfo.PartialD(astNodeName("M"), Set.of()),
                G0_C, new ParserInfo.PartialD(astNodeName("C"), Set.of()),
                N, new ParserInfo.Full(astNodeName("N")),
                G, new ParserInfo.Full(astNodeName("G")));
        var expectedAstMetadata = astMetadata.updateParserInfos(expectedParserInfoMap);

        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
        assertBnfSemanticEquals(expectedBnf, result.bnf());
        assertEquals(expectedAstMetadata, result.astMetadata());
    }

    @Test
    public void indirect_common_prefix_3_levels_and_first_variable() {
        var bnf = new BNF(Set.of(selection(A, B, C),
                                 selection(B, F, G),
                                 selection(F, M, N),
                                 derivation(M, X, mul, y),
                                 derivation(N, z),
                                 derivation(G, y),
                                 derivation(C, X, plus, y),
                                 derivation(X, x)),
                          A);
        var astMetadata = new AstGenerator(namer, bnf).generate().snd();

        var expectedBnf = new BNF(Set.of(selection(A, C_M, N, G),
                                         derivation(C_M, X, G0),
                                         selection(G0, G0_M, G0_C),
                                         derivation(G0_M, mul, y),
                                         derivation(G0_C, plus, y),
                                         derivation(N, z),
                                         derivation(G, y),
                                         derivation(X, x)),
                                  A);

        Map<Variable, ParserInfo> expectedParserInfoMap = mapOf(
                A, new ParserInfo.Full(astNodeName("A")),
                C_M, new ParserInfo.Full(astNodeName("A")),
                G0, new ParserInfo.PartialS(astNodeName("A"), Set.of(astNodeName("X"))),
                G0_M, new ParserInfo.PartialD(astNodeName("M"), Set.of("x0")),
                G0_C, new ParserInfo.PartialD(astNodeName("C"), Set.of("x0")),
                N, new ParserInfo.Full(astNodeName("N")),
                G, new ParserInfo.Full(astNodeName("G")),
                X, new ParserInfo.Full(astNodeName("X")));
        var expectedAstMetadata = astMetadata.updateParserInfos(expectedParserInfoMap);

        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
        assertBnfSemanticEquals(expectedBnf, result.bnf());
        assertEquals(expectedAstMetadata, result.astMetadata());
    }

    @Test
    public void multiple_common_prefixes() {
        var bnf = new BNF(Set.of(selection(A, B, C),
                                 selection(B, F, G),
                                 derivation(F, x, mul, y),
                                 selection(G, K, M),
                                 derivation(K, z, k),
                                 derivation(M, z, m),
                                 derivation(C, x, plus, y)),
                          A);
        var astMetadata = new AstGenerator(namer, bnf).generate().snd();

        var expectedBnf = new BNF(Set.of(selection(A, C_F, K_M),
                                         derivation(K_M, z, G0),
                                         selection(G0, G0_K, G0_M),
                                         derivation(G0_K, k),
                                         derivation(G0_M, m),
                                         derivation(C_F, x, G1),
                                         selection(G1, G1_F, G1_C),
                                         derivation(G1_F, mul, y),
                                         derivation(G1_C, plus, y)),
                                  A);

        Map<Variable, ParserInfo> expectedParserInfoMap = mapOf(
                A, new ParserInfo.Full(astNodeName("A")),
                C_F, new ParserInfo.Full(astNodeName("A")),
                K_M, new ParserInfo.Full(astNodeName("A")),
                G0, new ParserInfo.PartialS(astNodeName("A"), Set.of()),
                G0_K, new ParserInfo.PartialD(astNodeName("K"), Set.of()),
                G0_M, new ParserInfo.PartialD(astNodeName("M"), Set.of()),
                G1, new ParserInfo.PartialS(astNodeName("A"), Set.of()),
                G1_F, new ParserInfo.PartialD(astNodeName("F"), Set.of()),
                G1_C, new ParserInfo.PartialD(astNodeName("C"), Set.of())
        );
        var expectedAstMetadata = astMetadata.updateParserInfos(expectedParserInfoMap);

        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
        assertBnfSemanticEquals(expectedBnf, result.bnf());
        assertEquals(expectedAstMetadata, result.astMetadata());
    }

    ClassName astNodeName(CharSequence name) {
        return namer.astNodeClassName(name.toString());
    }

    @Test
    public void chain_of_length_1() {
        var bnf = new BNF(Set.of(selection(A, B, C),
                                 derivation(C, x),
                                 derivation(B, C, y)),
                          A);

        var astMetadata = new AstGenerator(namer, bnf).generate().snd();

        var expectedBnf = new BNF(Set.of(selection(A, A_B),
                                         derivation(A_B, C, G0),
                                         selection(G0, G0_A, G0_B),
                                         derivation(G0_A),
                                         derivation(G0_B, y),
                                         derivation(C, x)),
                          A);

        Map<Variable, ParserInfo> expectedParserInfoMap = mapOf(
                A, new ParserInfo.Full(astNodeName("A")),
                A_B, new ParserInfo.Full(astNodeName("A")),
                G0, new ParserInfo.PartialS(astNodeName("A"), Set.of(astNodeName("C"))),
                G0_A, new ParserInfo.PartialEmpty(astNodeName("C")),
                G0_B, new ParserInfo.PartialD(astNodeName("B"), Set.of("c0")),
                C, new ParserInfo.Full(astNodeName("C"))
        );
        var expectedAstMetadata = astMetadata.updateParserInfos(expectedParserInfoMap);

        var result = new LeftFactoring(namer).apply(new LeftFactoring.Data(bnf, astMetadata));
        assertBnfSemanticEquals(expectedBnf, result.bnf());
        assertEquals(expectedAstMetadata, result.astMetadata());
    }

}
