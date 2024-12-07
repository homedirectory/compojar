package lf.correct;

import java.util.function.Function;

interface LF_ApiImpl {
  class A_Impl<K> implements LF_Api.A<K> {
    private final Function<? super LF_AstNode.A, K> k;

    A_Impl(final Function<? super LF_AstNode.A, K> k) {
      this.k = k;
    }

    public LF_Api.G0<K> x() {
      return new A_B_Impl<>(k).x();
    }
  }

  class Y_Impl<K> implements LF_Api.Y<K> {
    private final K k;

    Y_Impl(final K k) {
      this.k = k;
    }

    public K y() {
      return this.k;
    }
  }

  class C_Impl<K> implements LF_Api.C<K> {
    private final Function<? super LF_AstNode.C, K> k;

    C_Impl(final Function<? super LF_AstNode.C, K> k) {
      this.k = k;
    }

    public K x() {
      return k.apply(new LF_AstNode.C());
    }
  }

  class A_B_Impl<K> extends C_Impl<LF_Api.G0<K>> implements LF_Api.A_B<K> {
    private final Function<? super LF_AstNode.A, K> k;

    A_B_Impl(final Function<? super LF_AstNode.A, K> k) {
      super(x0 -> new G0_Impl<>(k, x0));
      this.k = k;
    }
  }

  class G0_A_Impl<K> implements LF_Api.G0_A<K> {
    private final Function<? super LF_AstNode.C, K> k;

    private final LF_AstNode.C x;

    G0_A_Impl(final Function<? super LF_AstNode.C, K> k, final LF_AstNode.C x) {
      this.k = k;
      this.x = x;
    }

    public K $() {
      return k.apply(x);
    }
  }

  class G0_Impl<K> implements LF_Api.G0<K> {
    private final Function<? super LF_AstNode.A, K> k;

    private final LF_AstNode.C c0;

    G0_Impl(final Function<? super LF_AstNode.A, K> k, final LF_AstNode.C c0) {
      this.k = k;
      this.c0 = c0;
    }

    public K $() {
      return new G0_A_Impl<>(k, c0).$();
    }

    public K y() {
      return new G0_B_Impl<>(k, c0).y();
    }
  }

  class G0_B_Impl<K> extends Y_Impl<K> implements LF_Api.G0_B<K> {
    private final Function<? super LF_AstNode.A, K> k;

    private final LF_AstNode.C c0;

    G0_B_Impl(final Function<? super LF_AstNode.A, K> k, final LF_AstNode.C c0) {
      super(k.apply(c0));
      this.k = k;
      this.c0 = c0;
    }
  }
}
