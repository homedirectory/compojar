package math;

public interface Math_AstNode {
  class Sum implements E, Math_AstNode {
    public final N n0;

    public final N n1;

    public Sum(final N n0, final N n1) {
      this.n0 = n0;
      this.n1 = n1;
    }
  }

  class Prod implements E, Math_AstNode {
    public final N n0;

    public final N n1;

    public final N n2;

    public Prod(final N n0, final N n1, final N n2) {
      this.n0 = n0;
      this.n1 = n1;
      this.n2 = n2;
    }
  }

  class N2 implements N, Math_AstNode {
    public N2() {
    }
  }

  class N1 implements N, Math_AstNode {
    public N1() {
    }
  }

  class NegExpr implements E, Math_AstNode {
    public final E e0;

    public NegExpr(final E e0) {
      this.e0 = e0;
    }
  }

  interface N extends Math_AstNode {
  }

  interface E extends Math_AstNode {
  }
}
