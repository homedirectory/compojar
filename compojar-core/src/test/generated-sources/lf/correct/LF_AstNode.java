package lf.correct;

public interface LF_AstNode {
  class B implements A, LF_AstNode {
    public final C c0;

    public B(final C c0) {
      this.c0 = c0;
    }
  }

  class C implements A, LF_AstNode {
    public C() {
    }
  }

  interface A extends LF_AstNode {
  }
}
