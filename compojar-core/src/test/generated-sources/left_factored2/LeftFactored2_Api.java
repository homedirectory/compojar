package left_factored2;

import java.util.function.Function;

public interface LeftFactored2_Api {
  static A<LeftFactored2_AstNode.A> start() {
    return new LeftFactored2_ApiImpl.A_Impl<>(Function.identity());
  }

  interface A<K> extends PREF_RND71<K> {
  }

  interface PREF_RND71<K> {
    PREF_RND71_K<K> op(String op);
  }

  interface Partial_OP<K> {
    K $();
  }

  interface PREF_RND71_K<K> extends Partial_B<K>, Partial_C<K> {
  }

  interface Partial_D<K> {
    K $();
  }

  interface Partial_C<K> extends C<C<C<K>>> {
  }

  interface Partial_B<K> extends B<K> {
  }

  interface C<K> {
    K c();
  }

  interface B<K> {
    K b();
  }
}
