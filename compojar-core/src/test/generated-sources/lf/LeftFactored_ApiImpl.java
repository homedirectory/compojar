package lf;

import java.util.function.Function;

interface LeftFactored_ApiImpl {
  class O_C_Impl<K> extends PLUS_Impl<LeftFactored_Api.Y<K>> implements LeftFactored_Api.O_C<K> {
    private final Function<? super LeftFactored_AstNode.C, K> k;

    O_C_Impl(final Function<? super LeftFactored_AstNode.C, K> k) {
      super(new Y_Impl<>(k.apply(new LeftFactored_AstNode.C())));
      this.k = k;
    }
  }

  class Y_Impl<K> implements LeftFactored_Api.Y<K> {
    private final K k;

    Y_Impl(final K k) {
      this.k = k;
    }

    public K y() {
      return this.k;
    }
  }

  class MUL_Impl<K> implements LeftFactored_Api.MUL<K> {
    private final K k;

    MUL_Impl(final K k) {
      this.k = k;
    }

    public K mul() {
      return this.k;
    }
  }

  class A_Impl<K> implements LeftFactored_Api.A<K> {
    private final Function<? super LeftFactored_AstNode.A, K> k;

    A_Impl(final Function<? super LeftFactored_AstNode.A, K> k) {
      this.k = k;
    }

    public LeftFactored_Api.O<K> x() {
      return new F_C_Impl<>(k).x();
    }

    public K y() {
      return new G_Impl<>(k).y();
    }
  }

  class O_F_Impl<K> extends MUL_Impl<LeftFactored_Api.Y<K>> implements LeftFactored_Api.O_F<K> {
    private final Function<? super LeftFactored_AstNode.F, K> k;

    O_F_Impl(final Function<? super LeftFactored_AstNode.F, K> k) {
      super(new Y_Impl<>(k.apply(new LeftFactored_AstNode.F())));
      this.k = k;
    }
  }

  class O_Impl<K> implements LeftFactored_Api.O<K> {
    private final Function<? super LeftFactored_AstNode.A, K> k;

    O_Impl(final Function<? super LeftFactored_AstNode.A, K> k) {
      this.k = k;
    }

    public LeftFactored_Api.Y<K> mul() {
      return new O_F_Impl<>(k).mul();
    }

    public LeftFactored_Api.Y<K> plus() {
      return new O_C_Impl<>(k).plus();
    }
  }

  class G_Impl<K> implements LeftFactored_Api.G<K> {
    private final Function<? super LeftFactored_AstNode.G, K> k;

    G_Impl(final Function<? super LeftFactored_AstNode.G, K> k) {
      this.k = k;
    }

    public K y() {
      return k.apply(new LeftFactored_AstNode.G());
    }
  }

  class PLUS_Impl<K> implements LeftFactored_Api.PLUS<K> {
    private final K k;

    PLUS_Impl(final K k) {
      this.k = k;
    }

    public K plus() {
      return this.k;
    }
  }

  class F_C_Impl<K> implements LeftFactored_Api.F_C<K> {
    private final Function<? super LeftFactored_AstNode.A, K> k;

    F_C_Impl(final Function<? super LeftFactored_AstNode.A, K> k) {
      this.k = k;
    }

    public O_Impl<K> x() {
      return new O_Impl<>(k);
    }
  }
}
