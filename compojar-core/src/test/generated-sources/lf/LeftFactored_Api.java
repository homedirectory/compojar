package lf;

import java.util.function.Function;

public interface LeftFactored_Api {
  static A<LeftFactored_AstNode.A> start() {
    return new LeftFactored_ApiImpl.A_Impl<>(Function.identity());
  }

  interface O<K> extends O_F<K>, O_C<K> {
  }

  interface F_C<K> {
    O<K> x();
  }

  interface Y<K> {
    K y();
  }

  interface O_C<K> extends PLUS<Y<K>> {
  }

  interface MUL<K> {
    K mul();
  }

  interface G<K> {
    K y();
  }

  interface PLUS<K> {
    K plus();
  }

  interface A<K> extends F_C<K>, G<K> {
  }

  interface O_F<K> extends MUL<Y<K>> {
  }
}
