package lf;

import java.util.function.Function;

public interface LF_Api {
  static A<LF_AstNode.A> start() {
    return new LF_ApiImpl.A_Impl<>(Function.identity());
  }

  interface A_B<K> extends C<G0<K>> {
  }

  interface G0<K> extends G0_A<K>, G0_B<K> {
  }

  interface Y<K> {
    K y();
  }

  interface G0_A<K> {
    K $();
  }

  interface G0_B<K> extends Y<K> {
  }

  interface C<K> {
    K x();
  }

  interface A<K> extends A_B<K> {
  }
}
