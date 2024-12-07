package left_factored3;

public interface LeftFactored3_AstNode {
  class G implements S, LeftFactored3_AstNode {
    public final A a0;

    public G(final A a0) {
      this.a0 = a0;
    }
  }

  class C implements A, LeftFactored3_AstNode {
    public C() {
    }
  }

  class B implements A, LeftFactored3_AstNode {
    public B() {
    }
  }

  interface A extends LeftFactored3_AstNode {
  }

  interface S extends LeftFactored3_AstNode {
  }
}
