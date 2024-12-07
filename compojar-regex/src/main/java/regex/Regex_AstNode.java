package regex;

import java.lang.String;

public interface Regex_AstNode {
  class Optional implements QuantifiedTerm, Regex_AstNode {
    public final Term term;

    public Optional(final Term term) {
      this.term = term;
    }
  }

  class Space implements Term, Regex_AstNode {
    public Space() {
    }
  }

  class Group implements Term, Regex_AstNode {
    public final ExprList exprList;

    public Group(final ExprList exprList) {
      this.exprList = exprList;
    }
  }

  interface QuantifiedTerm extends Term, Regex_AstNode {
  }

  class Nil implements ExprList, Regex_AstNode {
    public Nil() {
    }
  }

  class Character implements Term, Regex_AstNode {
    public final String s;

    public Character(final String s) {
      this.s = s;
    }
  }

  class Digit implements Term, Regex_AstNode {
    public Digit() {
    }
  }

  class ZeroOrMore implements QuantifiedTerm, Regex_AstNode {
    public final Term term;

    public ZeroOrMore(final Term term) {
      this.term = term;
    }
  }

  class Between implements QuantifiedTerm, Regex_AstNode {
    public final int min;

    public final int max;

    public final Term term;

    public Between(final int min, final int max, final Term term) {
      this.min = min;
      this.max = max;
      this.term = term;
    }
  }

  class Str implements Term, Regex_AstNode {
    public final String s;

    public Str(final String s) {
      this.s = s;
    }
  }

  interface ExprList extends Regex_AstNode {
  }

  class ExprCons implements ExprList, Regex_AstNode {
    public final Term term;

    public final ExprList exprList;

    public ExprCons(final Term term, final ExprList exprList) {
      this.term = term;
      this.exprList = exprList;
    }
  }

  interface Term extends Regex_AstNode {
  }

  class AtLeast implements QuantifiedTerm, Regex_AstNode {
    public final int min;

    public final Term term;

    public AtLeast(final int min, final Term term) {
      this.min = min;
      this.term = term;
    }
  }

  class Times implements QuantifiedTerm, Regex_AstNode {
    public final int n;

    public final Term term;

    public Times(final int n, final Term term) {
      this.n = n;
      this.term = term;
    }
  }

  class OneOrMore implements QuantifiedTerm, Regex_AstNode {
    public final Term term;

    public OneOrMore(final Term term) {
      this.term = term;
    }
  }
}
