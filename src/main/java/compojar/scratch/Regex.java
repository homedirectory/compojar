package compojar.scratch;

import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;

import java.util.Set;

import static compojar.bnf.Rule.derivation;
import static compojar.bnf.Rule.selection;
import static compojar.bnf.Symbol.terminal;
import static compojar.bnf.Symbol.variable;

public interface Regex {

    Terminal a = terminal("a"),
            start = terminal("start"),
            end = terminal("end"),
            zeroOrMore = terminal("zeroOrMore"),
            optional = terminal("optional"),
            $ = terminal("$")
            ;

    Variable Term = variable("Term"),
            ExprList = variable("ExprList"),
            ExprCons = variable("ExprCons"),
            Nil = variable("Nil"),
            Str = variable("Str"),
            Group = variable("Group"),
            QuantifiedTerm = variable("QuantifiedTerm"),
            ZeroOrMore = variable("ZeroOrMore"),
            Optional = variable("Optional")
                    ;

    BNF bnf_lr = new BNF(Set.of(selection(ExprList, Nil, ExprCons),
                                derivation(ExprCons, Term, ExprList),
                                derivation(Nil),
                                selection(Term, Str, Group, QuantifiedTerm),
                                derivation(Str, a),
                                derivation(Group, start, Term, end),
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
                             derivation(Str, a),
                             derivation(Group, start, Term, end),
                             selection(QuantifiedTerm, ZeroOrMore, Optional),
                             derivation(ZeroOrMore, zeroOrMore, Term),
                             derivation(Optional, optional, Term)),
                      ExprList);

}
