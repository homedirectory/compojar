package compojar.scratch;

import compojar.gen.Generator;
import compojar.gen.Namer;

import java.io.IOException;
import java.nio.file.Path;

public class Regex_Gen {

    public static void main(String[] args)
            throws IOException
    {
        var namer = new Namer("Regex", "regex");
        var originalBnf = Regex.bnf;

        Path destPath = Path.of("src/test/generated-sources/").toAbsolutePath();
        new Generator(namer, originalBnf).generate(destPath);
    }

}
