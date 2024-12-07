package math;

import java.util.function.Function;

interface Math_ApiImpl {
  class N2_Impl<K> implements Math_Api.N2<K> {
    private final Function<? super Math_AstNode.N2, K> k;

    N2_Impl(final Function<? super Math_AstNode.N2, K> k) {
      this.k = k;
    }

    public K zero() {
      return k.apply(new Math_AstNode.N2());
    }
  }

  class O2_Impl<K> implements Math_Api.O2<K> {
    private final Function<? super Math_AstNode.Prod, K> k;

    private final Math_AstNode.N n0;

    O2_Impl(final Function<? super Math_AstNode.Prod, K> k, final Math_AstNode.N n0) {
      this.k = k;
      this.n0 = n0;
    }

    public N_Impl<Math_Api.M<Math_Api.N<K>>> mul() {
      return new N_Impl<>(x0 -> new M_Impl<>(new N_Impl<>(x1 -> k.apply(new Math_AstNode.Prod(n0, x0, x1)))));
    }
  }

  class N1_Impl<K> implements Math_Api.N1<K> {
    private final Function<? super Math_AstNode.N1, K> k;

    N1_Impl(final Function<? super Math_AstNode.N1, K> k) {
      this.k = k;
    }

    public K one() {
      return k.apply(new Math_AstNode.N1());
    }
  }

  class N_Impl<K> implements Math_Api.N<K> {
    private final Function<? super Math_AstNode.N, K> k;

    N_Impl(final Function<? super Math_AstNode.N, K> k) {
      this.k = k;
    }

    public K one() {
      return new N1_Impl<>(k).one();
    }

    public K zero() {
      return new N2_Impl<>(k).zero();
    }
  }

  class O1_Impl<K> implements Math_Api.O1<K> {
    private final Function<? super Math_AstNode.Sum, K> k;

    private final Math_AstNode.N n0;

    O1_Impl(final Function<? super Math_AstNode.Sum, K> k, final Math_AstNode.N n0) {
      this.k = k;
      this.n0 = n0;
    }

    public N_Impl<K> plus() {
      return new N_Impl<>(x0 -> k.apply(new Math_AstNode.Sum(n0, x0)));
    }
  }

  class NegExpr_Impl<K> implements Math_Api.NegExpr<K> {
    private final Function<? super Math_AstNode.NegExpr, K> k;

    NegExpr_Impl(final Function<? super Math_AstNode.NegExpr, K> k) {
      this.k = k;
    }

    public E_Impl<K> neg() {
      return new E_Impl<>(x0 -> k.apply(new Math_AstNode.NegExpr(x0)));
    }
  }

  class Prod_Sum_Impl<K> extends N_Impl<Math_Api.O<K>> implements Math_Api.Prod_Sum<K> {
    private final Function<? super Math_AstNode.E, K> k;

    Prod_Sum_Impl(final Function<? super Math_AstNode.E, K> k) {
      super(x0 -> new O_Impl<>(k, x0));
      this.k = k;
    }
  }

  class E_Impl<K> implements Math_Api.E<K> {
    private final Function<? super Math_AstNode.E, K> k;

    E_Impl(final Function<? super Math_AstNode.E, K> k) {
      this.k = k;
    }

    public Math_Api.E<K> neg() {
      return new NegExpr_Impl<>(k).neg();
    }

    public Math_Api.O<K> one() {
      return new Prod_Sum_Impl<>(k).one();
    }

    public Math_Api.O<K> zero() {
      return new Prod_Sum_Impl<>(k).zero();
    }
  }

  class M_Impl<K> implements Math_Api.M<K> {
    private final K k;

    M_Impl(final K k) {
      this.k = k;
    }

    public K mul() {
      return this.k;
    }
  }

  class O_Impl<K> implements Math_Api.O<K> {
    private final Function<? super Math_AstNode.E, K> k;

    private final Math_AstNode.N c0;

    O_Impl(final Function<? super Math_AstNode.E, K> k, final Math_AstNode.N c0) {
      this.k = k;
      this.c0 = c0;
    }

    public Math_Api.N<K> plus() {
      return new O1_Impl<>(k, c0).plus();
    }

    public Math_Api.N<Math_Api.M<Math_Api.N<K>>> mul() {
      return new O2_Impl<>(k, c0).mul();
    }
  }
}
