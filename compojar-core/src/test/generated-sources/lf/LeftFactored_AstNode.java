package lf;

public interface LeftFactored_AstNode {
  interface B extends A, LeftFactored_AstNode {
  }

  interface A extends LeftFactored_AstNode {
  }

  class C implements A, LeftFactored_AstNode {
    C() {
    }
  }

  class F implements B, LeftFactored_AstNode {
    F() {
    }
  }

  class G implements B, LeftFactored_AstNode {
    G() {
    }
  }
}
