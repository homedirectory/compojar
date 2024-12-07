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
import static compojar.scratch.Regex.T.*;
import static compojar.scratch.Regex.V.*;

public interface Regex {

    enum T implements Terminal {
        begin, end, str, zeroOrMore, oneOrMore, optional, digit, space, times, character, atLeast, between, $
    }

    enum V implements Variable {
        Term, ExprList, ExprCons, Nil, Group, Str, Digit, Space, Character,
        QuantifiedTerm, ZeroOrMore, OneOrMore, Optional, Between, AtLeast, Times

    }

    BNF bnf = new BNF(
            // rules
            Set.of(selection(ExprList, Nil, ExprCons),
                   derivation(ExprCons, Term, ExprList),
                   derivation(Nil, $),
                   selection(Term, Str, Group, QuantifiedTerm, Digit, Space, Character),
                   derivation(Str, str.parameters(String.class, "s")),
                   derivation(Group, begin, ExprList, end),
                   derivation(Digit, digit), // \d
                   derivation(Space, space), // \s
                   derivation(Character, character.parameters(String.class, "s")), // [abc]
                   selection(QuantifiedTerm, ZeroOrMore, OneOrMore, Optional, Times, Between, AtLeast),
                   derivation(Times, times.parameters(int.class, "n"), Term), // {n}
                   derivation(Between, between.parameters(int.class, "min", int.class, "max"), Term), // {min, max}
                   derivation(AtLeast, atLeast.parameters(int.class, "min"), Term), // {min,}
                   derivation(ZeroOrMore, zeroOrMore, Term), // *
                   derivation(OneOrMore, oneOrMore, Term), // +
                   derivation(Optional, optional, Term) // ?
            ),
            /*start*/ ExprList);

    public static void main(String[] args)
            throws IOException
    {
        var namer = new Namer("Regex", "regex");
        var originalBnf = Regex.bnf;

        Path destPath = Path.of("../compojar-regex/src/main/java/").toAbsolutePath();
        new Generator(namer, originalBnf).generate(destPath);
    }
}
