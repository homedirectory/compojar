package left_factored2;

import java.lang.String;

public interface LeftFactored2_AstNode {
  class C implements A, LeftFactored2_AstNode {
    public final D d;

    public C(final D d) {
      this.d = d;
    }
  }

  class Z implements LeftFactored2_AstNode {
    public Z() {
    }
  }

  class D implements LeftFactored2_AstNode {
    public final OP oP;

    public D(final OP oP) {
      this.oP = oP;
    }
  }

  class B implements A, LeftFactored2_AstNode {
    public final D d;

    public B(final D d) {
      this.d = d;
    }
  }

  class Y implements LeftFactored2_AstNode {
    public Y() {
    }
  }

  interface A extends LeftFactored2_AstNode {
  }

  class OP implements LeftFactored2_AstNode {
    public final String op;

    public OP(final String op) {
      this.op = op;
    }
  }
}
