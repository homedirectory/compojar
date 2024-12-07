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
import static compojar.scratch.CS.T.*;
import static compojar.scratch.CS.V.*;

public class CS {

    //    Constraint     ::= Equation | Inequality
    //    Equation       ::= Expression "=" Expression
    //    Inequality     ::= Expression "<" Expression
    //                   | Expression ">" Expression
    //                   | Expression "<=" Expression
    //                   | Expression ">=" Expression
    //    Expression     ::= Term (("+" | "-") Term)*
    //    Term           ::= Factor (("*" | "/") Factor)*
    //    Factor         ::= Var | Num | Group
    //    Group          ::= "(" Expression ")"
    //    Var            ::= var(String)
    //    Num            ::= num(long)


    public enum T implements Terminal {
        begin, end, var, num, plus, notEq, eq, constr,
    }

    public enum V implements Variable {
        ConstraintLang, Constraint, Equation, Inequality,
        Expression,
        Var, Num, Term, Group,
    }

    public static final BNF bnf = new BNF(
            Set.of(derivation(ConstraintLang, constr, Constraint),
                   selection(Constraint, V.Equation, Inequality),
                   derivation(Equation, Expression, eq, Expression),
                   derivation(Inequality, Expression, notEq, Expression),
                   derivation(Expression, Term, plus, Term),
                   selection(Term, Var, Num, Group),
                   derivation(Var, var.parameters(String.class, "s")),
                   derivation(Group, begin, Term, end),
                   derivation(Num, num.parameters(long.class, "n"))),
            Constraint);

    public static void main(String[] args)
            throws IOException
    {
        var namer = new Namer("CS", "cs");
        Path destPath = Path.of("src/test/generated-sources/").toAbsolutePath();
        new Generator(namer, bnf).generate(destPath);
    }


}
