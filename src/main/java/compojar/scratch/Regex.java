package compojar.scratch;

import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;

import java.util.Set;

import static compojar.bnf.Rule.derivation;
import static compojar.bnf.Rule.selection;
import static compojar.scratch.Regex.T.*;
import static compojar.scratch.Regex.V.*;

public interface Regex {

    enum T implements Terminal {
        str, begin, end, zeroOrMore, optional, $
    }

    enum V implements Variable {
        Term,
        ExprList,
        ExprCons,
        Nil,
        Str,
        Group,
        QuantifiedTerm,
        ZeroOrMore,
        Optional,
    }

    BNF bnf_lr = new BNF(Set.of(selection(ExprList, Nil, ExprCons),
                                derivation(ExprCons, Term, ExprList),
                                derivation(Nil),
                                selection(Term, Str, Group, QuantifiedTerm),
                                derivation(Str, str),
                                derivation(Group, begin, Term, end),
                                selection(QuantifiedTerm, ZeroOrMore, Optional),
                                // Left recursion with Term.
                                // Could be rewritten as ZeroOrMore -> zeroOrMore Term
                                derivation(ZeroOrMore, Term, zeroOrMore),
                                // Could be rewritten as Optional -> optional Term
                                derivation(Optional, Term, optional)),
                         Term);

    BNF bnf = new BNF(Set.of(selection(ExprList, Nil, ExprCons),
                             derivation(ExprCons, Term, ExprList),
                             // Illegal rule: empty RHS.
//                             derivation(Nil),
                             derivation(Nil, $),
                             selection(Term, Str, Group, QuantifiedTerm),
                             derivation(Str, str.parameters(String.class, "s")),
                             derivation(Group, begin, Term, end),
                             selection(QuantifiedTerm, ZeroOrMore, Optional),
                             derivation(ZeroOrMore, zeroOrMore, Term),
                             derivation(Optional, optional, Term)),
                      ExprList);

}
