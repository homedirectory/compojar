package left_factored2;

import java.lang.String;
import java.util.function.Function;

interface LeftFactored2_ApiImpl {
  class A_Impl<K> implements LeftFactored2_Api.A {
    private final Function<? super LeftFactored2_AstNode.A, K> k;

    A_Impl(final Function<? super LeftFactored2_AstNode.A, K> k) {
      this.k = k;
    }

    public LeftFactored2_Api.PREF_RND71_K<K> op(String op) {
      return new PREF_RND71_Impl<>(k).op(op);
    }
  }

  class Partial_OP_Impl<K> implements LeftFactored2_Api.Partial_OP {
    private final Function<? super LeftFactored2_AstNode.OP, K> k;

    private final String op;

    Partial_OP_Impl(final Function<? super LeftFactored2_AstNode.OP, K> k, final String op) {
      this.k = k;
      this.op = op;
    }

    public K $() {
      return k.apply(new LeftFactored2_AstNode.OP(op));
    }
  }

  class PREF_RND71_K_Impl<K> implements LeftFactored2_Api.PREF_RND71_K {
    private final Function<? super LeftFactored2_AstNode.A, K> k;

    private final String p0;

    PREF_RND71_K_Impl(final Function<? super LeftFactored2_AstNode.A, K> k, final String p0) {
      this.k = k;
      this.p0 = p0;
    }

    public K b() {
      return new Partial_B_Impl<>(k, p0).b();
    }

    public LeftFactored2_Api.C<LeftFactored2_Api.C<K>> c() {
      return new Partial_C_Impl<>(k, p0).c();
    }
  }

  class PREF_RND71_Impl<K> implements LeftFactored2_Api.PREF_RND71 {
    private final Function<? super LeftFactored2_AstNode.A, K> k;

    PREF_RND71_Impl(final Function<? super LeftFactored2_AstNode.A, K> k) {
      this.k = k;
    }

    public PREF_RND71_K_Impl<K> op(String op) {
      return new PREF_RND71_K_Impl<>(k, op);
    }
  }

  class Partial_D_Impl<K> implements LeftFactored2_Api.Partial_D {
    private final Function<? super LeftFactored2_AstNode.D, K> k;

    private final String op;

    Partial_D_Impl(final Function<? super LeftFactored2_AstNode.D, K> k, final String op) {
      this.k = k;
      this.op = op;
    }

    public K $() {
      return new Partial_OP_Impl<>(x0 -> k.apply(new LeftFactored2_AstNode.D(x0)), op).$();
    }
  }

  class Partial_B_Impl<K> extends B_Impl<K> implements LeftFactored2_Api.Partial_B {
    private final Function<? super LeftFactored2_AstNode.B, K> k;

    private final String op;

    Partial_B_Impl(final Function<? super LeftFactored2_AstNode.B, K> k, final String op) {
      super(new Partial_D_Impl<>(x0 -> k.apply(new LeftFactored2_AstNode.B(x0)), op).$());
      this.k = k;
      this.op = op;
    }
  }

  class Partial_C_Impl<K> extends C_Impl<LeftFactored2_Api.C<LeftFactored2_Api.C<K>>> implements LeftFactored2_Api.Partial_C {
    private final Function<? super LeftFactored2_AstNode.C, K> k;

    private final String op;

    Partial_C_Impl(final Function<? super LeftFactored2_AstNode.C, K> k, final String op) {
      super(new Partial_D_Impl<>(x0 -> new C_Impl<>(new C_Impl<>(k.apply(new LeftFactored2_AstNode.C(x0)))), op).$());
      this.k = k;
      this.op = op;
    }
  }

  class B_Impl<K> implements LeftFactored2_Api.B {
    private final K k;

    B_Impl(final K k) {
      this.k = k;
    }

    public K b() {
      return this.k;
    }
  }

  class C_Impl<K> implements LeftFactored2_Api.C {
    private final K k;

    C_Impl(final K k) {
      this.k = k;
    }

    public K c() {
      return this.k;
    }
  }
}
