package left_factored3;

import java.util.function.Function;

public interface LeftFactored3_Api {
  static A<LeftFactored3_AstNode.A> start() {
    return new LeftFactored3_ApiImpl.A_Impl<>(Function.identity());
  }

  interface PREF_RND71<K> {
    PREF_RND71_K<K> a();
  }

  interface PREF_RND71_K<K> extends Partial_C<K>, Partial_B<K> {
  }

  interface Partial_C<K> {
    K c();
  }

  interface A<K> extends PREF_RND71<K> {
  }

  interface Partial_B<K> extends B<K> {
  }

  interface B<K> {
    K b();
  }
}
