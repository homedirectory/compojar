package regex;

import java.util.function.Function;

public interface Regex_Api {
  static ExprList<Regex_AstNode.ExprList> start() {
    return new Regex_ApiImpl.ExprList_Impl<>(Function.identity());
  }

  interface QuantifiedTerm<K> extends Times<K>, OneOrMore<K>, ZeroOrMore<K>, Optional<K>, AtLeast<K>, Between<K> {
  }

  interface Group<K> {
    ExprList<END<K>> begin();
  }

  interface Space<K> {
    K space();
  }

  interface ZeroOrMore<K> {
    Term<K> zeroOrMore();
  }

  interface END<K> {
    K end();
  }

  interface AtLeast<K> {
    Term<K> atLeast(int min);
  }

  interface Between<K> {
    Term<K> between(int min, int max);
  }

  interface Times<K> {
    Term<K> times(int n);
  }

  interface OneOrMore<K> {
    Term<K> oneOrMore();
  }

  interface ExprList<K> extends Nil<K>, ExprCons<K> {
  }

  interface Optional<K> {
    Term<K> optional();
  }

  interface Character<K> {
    K character(String s);
  }

  interface Nil<K> {
    K $();
  }

  interface Digit<K> {
    K digit();
  }

  interface Str<K> {
    K str(String s);
  }

  interface Term<K> extends Space<K>, Str<K>, Group<K>, Digit<K>, Character<K>, QuantifiedTerm<K> {
  }

  interface ExprCons<K> extends Term<ExprList<K>> {
  }
}
