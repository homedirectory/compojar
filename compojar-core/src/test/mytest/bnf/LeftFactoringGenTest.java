package compojar.bnf;

import compojar.gen.Generator;
import compojar.gen.Namer;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static compojar.bnf.Rule.derivation;
import static compojar.bnf.Rule.selection;
import static compojar.bnf.Symbol.terminal;
import static compojar.bnf.Symbol.variable;

public class LeftFactoringGenTest {

    static final Terminal
            x = terminal("x"), y = terminal("y"), mul = terminal("mul"), plus = terminal("plus"),
            z = terminal("z"),
            k = terminal("k"),
            m = terminal("m")
                    ;
    static final Variable
            A = variable("A"), B = variable("B"), C = variable("C"), F = variable("F"), G = variable("G"),
            A_B = variable("A_B"),
            F_C = variable("F_C"),
            K_M = variable("K_M"),
            M_C = variable("M_C"),
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

    @Test
    public void a()
            throws IOException
    {
        var namer = new Namer("LF", "lf");
        var bnf = new BNF(Set.of(selection(A, B, C),
                                 derivation(C, x),
                                 derivation(B, C, y)),
                          A);
        var generator = new Generator(namer, bnf);
        generator.generate(Path.of("src/test/generated-sources"));
    }

}
