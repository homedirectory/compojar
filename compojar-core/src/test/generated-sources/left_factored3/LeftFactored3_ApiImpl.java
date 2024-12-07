package left_factored3;

import java.util.function.Function;

interface LeftFactored3_ApiImpl {
  class Partial_B_Impl<K> extends B_Impl<K> implements LeftFactored3_Api.Partial_B<K> {
    private final Function<? super LeftFactored3_AstNode.B, K> k;

    Partial_B_Impl(final Function<? super LeftFactored3_AstNode.B, K> k) {
      super(k.apply(new LeftFactored3_AstNode.B()));
      this.k = k;
    }
  }

  class Partial_C_Impl<K> implements LeftFactored3_Api.Partial_C<K> {
    private final Function<? super LeftFactored3_AstNode.C, K> k;

    Partial_C_Impl(final Function<? super LeftFactored3_AstNode.C, K> k) {
      this.k = k;
    }

    public K c() {
      return k.apply(new LeftFactored3_AstNode.C());
    }
  }

  class PREF_RND71_K_Impl<K> implements LeftFactored3_Api.PREF_RND71_K<K> {
    private final Function<? super LeftFactored3_AstNode.A, K> k;

    PREF_RND71_K_Impl(final Function<? super LeftFactored3_AstNode.A, K> k) {
      this.k = k;
    }

    public K c() {
      return new Partial_C_Impl<>(k).c();
    }

    public K b() {
      return new Partial_B_Impl<>(k).b();
    }
  }

  class A_Impl<K> implements LeftFactored3_Api.A<K> {
    private final Function<? super LeftFactored3_AstNode.A, K> k;

    A_Impl(final Function<? super LeftFactored3_AstNode.A, K> k) {
      this.k = k;
    }

    public LeftFactored3_Api.PREF_RND71_K<K> a() {
      return new PREF_RND71_Impl<>(k).a();
    }
  }

  class B_Impl<K> implements LeftFactored3_Api.B<K> {
    private final K k;

    B_Impl(final K k) {
      this.k = k;
    }

    public K b() {
      return this.k;
    }
  }

  class PREF_RND71_Impl<K> implements LeftFactored3_Api.PREF_RND71<K> {
    private final Function<? super LeftFactored3_AstNode.A, K> k;

    PREF_RND71_Impl(final Function<? super LeftFactored3_AstNode.A, K> k) {
      this.k = k;
    }

    public PREF_RND71_K_Impl<K> a() {
      return new PREF_RND71_K_Impl<>(k);
    }
  }
}
