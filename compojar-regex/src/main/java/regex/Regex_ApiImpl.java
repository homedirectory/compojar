package regex;

import java.lang.String;
import java.util.function.Function;

interface Regex_ApiImpl {
  class Optional_Impl<K> implements Regex_Api.Optional<K> {
    private final Function<? super Regex_AstNode.Optional, K> k;

    Optional_Impl(final Function<? super Regex_AstNode.Optional, K> k) {
      this.k = k;
    }

    public Term_Impl<K> optional() {
      return new Term_Impl<>(x0 -> k.apply(new Regex_AstNode.Optional(x0)));
    }
  }

  class Between_Impl<K> implements Regex_Api.Between<K> {
    private final Function<? super Regex_AstNode.Between, K> k;

    Between_Impl(final Function<? super Regex_AstNode.Between, K> k) {
      this.k = k;
    }

    public Term_Impl<K> between(int min, int max) {
      return new Term_Impl<>(x0 -> k.apply(new Regex_AstNode.Between(min, max, x0)));
    }
  }

  class ExprCons_Impl<K> extends Term_Impl<Regex_Api.ExprList<K>> implements Regex_Api.ExprCons<K> {
    private final Function<? super Regex_AstNode.ExprCons, K> k;

    ExprCons_Impl(final Function<? super Regex_AstNode.ExprCons, K> k) {
      super(x0 -> new ExprList_Impl<>(x1 -> k.apply(new Regex_AstNode.ExprCons(x0, x1))));
      this.k = k;
    }
  }

  class Term_Impl<K> implements Regex_Api.Term<K> {
    private final Function<? super Regex_AstNode.Term, K> k;

    Term_Impl(final Function<? super Regex_AstNode.Term, K> k) {
      this.k = k;
    }

    public K space() {
      return new Space_Impl<>(k).space();
    }

    public K str(String s) {
      return new Str_Impl<>(k).str(s);
    }

    public Regex_Api.ExprList<Regex_Api.END<K>> begin() {
      return new Group_Impl<>(k).begin();
    }

    public K digit() {
      return new Digit_Impl<>(k).digit();
    }

    public K character(String s) {
      return new Character_Impl<>(k).character(s);
    }

    public Regex_Api.Term<K> times(int n) {
      return new QuantifiedTerm_Impl<>(k).times(n);
    }

    public Regex_Api.Term<K> oneOrMore() {
      return new QuantifiedTerm_Impl<>(k).oneOrMore();
    }

    public Regex_Api.Term<K> zeroOrMore() {
      return new QuantifiedTerm_Impl<>(k).zeroOrMore();
    }

    public Regex_Api.Term<K> optional() {
      return new QuantifiedTerm_Impl<>(k).optional();
    }

    public Regex_Api.Term<K> atLeast(int min) {
      return new QuantifiedTerm_Impl<>(k).atLeast(min);
    }

    public Regex_Api.Term<K> between(int min, int max) {
      return new QuantifiedTerm_Impl<>(k).between(min, max);
    }
  }

  class Str_Impl<K> implements Regex_Api.Str<K> {
    private final Function<? super Regex_AstNode.Str, K> k;

    Str_Impl(final Function<? super Regex_AstNode.Str, K> k) {
      this.k = k;
    }

    public K str(String s) {
      return k.apply(new Regex_AstNode.Str(s));
    }
  }

  class Nil_Impl<K> implements Regex_Api.Nil<K> {
    private final Function<? super Regex_AstNode.Nil, K> k;

    Nil_Impl(final Function<? super Regex_AstNode.Nil, K> k) {
      this.k = k;
    }

    public K $() {
      return k.apply(new Regex_AstNode.Nil());
    }
  }

  class ExprList_Impl<K> implements Regex_Api.ExprList<K> {
    private final Function<? super Regex_AstNode.ExprList, K> k;

    ExprList_Impl(final Function<? super Regex_AstNode.ExprList, K> k) {
      this.k = k;
    }

    public K $() {
      return new Nil_Impl<>(k).$();
    }

    public Regex_Api.ExprList<K> space() {
      return new ExprCons_Impl<>(k).space();
    }

    public Regex_Api.ExprList<K> str(String s) {
      return new ExprCons_Impl<>(k).str(s);
    }

    public Regex_Api.ExprList<Regex_Api.END<Regex_Api.ExprList<K>>> begin() {
      return new ExprCons_Impl<>(k).begin();
    }

    public Regex_Api.ExprList<K> digit() {
      return new ExprCons_Impl<>(k).digit();
    }

    public Regex_Api.ExprList<K> character(String s) {
      return new ExprCons_Impl<>(k).character(s);
    }

    public Regex_Api.Term<Regex_Api.ExprList<K>> times(int n) {
      return new ExprCons_Impl<>(k).times(n);
    }

    public Regex_Api.Term<Regex_Api.ExprList<K>> oneOrMore() {
      return new ExprCons_Impl<>(k).oneOrMore();
    }

    public Regex_Api.Term<Regex_Api.ExprList<K>> zeroOrMore() {
      return new ExprCons_Impl<>(k).zeroOrMore();
    }

    public Regex_Api.Term<Regex_Api.ExprList<K>> optional() {
      return new ExprCons_Impl<>(k).optional();
    }

    public Regex_Api.Term<Regex_Api.ExprList<K>> atLeast(int min) {
      return new ExprCons_Impl<>(k).atLeast(min);
    }

    public Regex_Api.Term<Regex_Api.ExprList<K>> between(int min, int max) {
      return new ExprCons_Impl<>(k).between(min, max);
    }
  }

  class Group_Impl<K> implements Regex_Api.Group<K> {
    private final Function<? super Regex_AstNode.Group, K> k;

    Group_Impl(final Function<? super Regex_AstNode.Group, K> k) {
      this.k = k;
    }

    public ExprList_Impl<Regex_Api.END<K>> begin() {
      return new ExprList_Impl<>(x0 -> new END_Impl<>(k.apply(new Regex_AstNode.Group(x0))));
    }
  }

  class ZeroOrMore_Impl<K> implements Regex_Api.ZeroOrMore<K> {
    private final Function<? super Regex_AstNode.ZeroOrMore, K> k;

    ZeroOrMore_Impl(final Function<? super Regex_AstNode.ZeroOrMore, K> k) {
      this.k = k;
    }

    public Term_Impl<K> zeroOrMore() {
      return new Term_Impl<>(x0 -> k.apply(new Regex_AstNode.ZeroOrMore(x0)));
    }
  }

  class Character_Impl<K> implements Regex_Api.Character<K> {
    private final Function<? super Regex_AstNode.Character, K> k;

    Character_Impl(final Function<? super Regex_AstNode.Character, K> k) {
      this.k = k;
    }

    public K character(String s) {
      return k.apply(new Regex_AstNode.Character(s));
    }
  }

  class Times_Impl<K> implements Regex_Api.Times<K> {
    private final Function<? super Regex_AstNode.Times, K> k;

    Times_Impl(final Function<? super Regex_AstNode.Times, K> k) {
      this.k = k;
    }

    public Term_Impl<K> times(int n) {
      return new Term_Impl<>(x0 -> k.apply(new Regex_AstNode.Times(n, x0)));
    }
  }

  class END_Impl<K> implements Regex_Api.END<K> {
    private final K k;

    END_Impl(final K k) {
      this.k = k;
    }

    public K end() {
      return this.k;
    }
  }

  class Space_Impl<K> implements Regex_Api.Space<K> {
    private final Function<? super Regex_AstNode.Space, K> k;

    Space_Impl(final Function<? super Regex_AstNode.Space, K> k) {
      this.k = k;
    }

    public K space() {
      return k.apply(new Regex_AstNode.Space());
    }
  }

  class QuantifiedTerm_Impl<K> implements Regex_Api.QuantifiedTerm<K> {
    private final Function<? super Regex_AstNode.QuantifiedTerm, K> k;

    QuantifiedTerm_Impl(final Function<? super Regex_AstNode.QuantifiedTerm, K> k) {
      this.k = k;
    }

    public Regex_Api.Term<K> times(int n) {
      return new Times_Impl<>(k).times(n);
    }

    public Regex_Api.Term<K> oneOrMore() {
      return new OneOrMore_Impl<>(k).oneOrMore();
    }

    public Regex_Api.Term<K> zeroOrMore() {
      return new ZeroOrMore_Impl<>(k).zeroOrMore();
    }

    public Regex_Api.Term<K> optional() {
      return new Optional_Impl<>(k).optional();
    }

    public Regex_Api.Term<K> atLeast(int min) {
      return new AtLeast_Impl<>(k).atLeast(min);
    }

    public Regex_Api.Term<K> between(int min, int max) {
      return new Between_Impl<>(k).between(min, max);
    }
  }

  class Digit_Impl<K> implements Regex_Api.Digit<K> {
    private final Function<? super Regex_AstNode.Digit, K> k;

    Digit_Impl(final Function<? super Regex_AstNode.Digit, K> k) {
      this.k = k;
    }

    public K digit() {
      return k.apply(new Regex_AstNode.Digit());
    }
  }

  class OneOrMore_Impl<K> implements Regex_Api.OneOrMore<K> {
    private final Function<? super Regex_AstNode.OneOrMore, K> k;

    OneOrMore_Impl(final Function<? super Regex_AstNode.OneOrMore, K> k) {
      this.k = k;
    }

    public Term_Impl<K> oneOrMore() {
      return new Term_Impl<>(x0 -> k.apply(new Regex_AstNode.OneOrMore(x0)));
    }
  }

  class AtLeast_Impl<K> implements Regex_Api.AtLeast<K> {
    private final Function<? super Regex_AstNode.AtLeast, K> k;

    AtLeast_Impl(final Function<? super Regex_AstNode.AtLeast, K> k) {
      this.k = k;
    }

    public Term_Impl<K> atLeast(int min) {
      return new Term_Impl<>(x0 -> k.apply(new Regex_AstNode.AtLeast(min, x0)));
    }
  }
}
