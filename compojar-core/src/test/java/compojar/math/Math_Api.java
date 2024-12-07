package compojar.math;

import java.util.function.Function;

public interface Math_Api {
  static E<Math_AstNode.E> start() {
    return new Math_ApiImpl.E_Impl<>(Function.identity());
  }

  interface O<K> extends O1<K>, O2<K> {
  }

  interface N<K> extends N1<K>, N2<K> {
  }

  interface M<K> {
    K mul();
  }

  interface NegExpr<K> {
    E<K> neg();
  }

  interface E<K> extends NegExpr<K>, Prod_Sum<K> {
  }

  interface O2<K> {
    N<M<N<K>>> mul();
  }

  interface N1<K> {
    K one();
  }

  interface N2<K> {
    K zero();
  }

  interface Prod_Sum<K> extends N<O<K>> {
  }

  interface O1<K> {
    N<K> plus();
  }
}
