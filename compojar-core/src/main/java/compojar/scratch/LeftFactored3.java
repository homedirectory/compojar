package compojar.scratch;

import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;
import compojar.gen.Generator;
import compojar.gen.Namer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static compojar.bnf.Rule.derivation;
import static compojar.bnf.Rule.selection;
import static compojar.scratch.LeftFactored3.T.*;
import static compojar.scratch.LeftFactored3.V.*;

public class LeftFactored3 {

    public static void main(String[] args)
            throws IOException {
        var namer = new Namer("LeftFactored3", "left_factored3");

        Path destPath = Path.of("src/test/generated-sources/").toAbsolutePath();
        new Generator(namer, bnf).generate(destPath);
    }

    enum V implements Variable {
        A, B, C, D, OP, Y, Z, S, G
    }

    enum T implements Terminal {
        y, z, plus, a, c, b;
    }

    static final BNF bnf = new BNF(
            Set.of(selection(S, G),
                   derivation(G, A),
                   selection(A, B, C),
                   derivation(B, a),
                   derivation(C, a, c)),
            A);

}
