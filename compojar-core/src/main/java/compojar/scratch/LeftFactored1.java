package compojar.scratch;

import java.util.function.Function;

public interface LeftFactored1 {

    // <A> ::= B | C
    // B ::= OP y
    // C ::= OP z
    // OP ::= plus

    interface Ast {
        interface A {}
        record B (OP op) implements A {}
        record C (OP op) implements A {}
        record OP() {}
    }

    // <A> ::= <PREF_RND71>
    // PREF_RND71 ::= plus <PREF_RND71_K>
    // <PREF_RND71_K> ::= <Partial_B> | <Partial_C>
    // Partial_B ::= <Partial_OP> y
    // Partial_C ::= <Partial_OP> z
    // Partial_OP ::= <empty>

    // Inline empty

    // <A> ::= <PREF_RND71>
    // PREF_RND71 ::= plus <PREF_RND71_K>
    // <PREF_RND71_K> ::= <Partial_B> | <Partial_C>
    // Partial_B ::= y  # implicit Partial_OP
    // Partial_C ::= z  # implicit Partial_OP
    // Partial_OP ::= <empty>

    interface Api {

        interface A<K> extends PREF_RND71<K> {}

        interface PREF_RND71<K> {
            PREF_RND71_K<K> plus();
        }

        interface PREF_RND71_K<K> extends Partial_B<K>
                , Partial_C<K>
                // , Partial_E<K>
        {}

        interface Partial_B<K> extends Y<K> {}

        interface Partial_C<K> extends Z<K> {}

        // interface Partial_D<K> extends X<K> {}

        // interface Partial_E<K>
        interface Partial_OP<K> {
            K $();
        }

        interface Y<K> {
            K y();
        }
        interface Z<K> {
            K z();
        }
        // interface X<K> {
        //     K x();
        // }
        // interface E<K> {}

    }

    class A_Impl<K> implements Api.A<K> {
        private final Function<? super Ast.A, K> k;

        public A_Impl(final Function<? super Ast.A, K> k) {
            this.k = k;
        }

        @Override
        public Api.PREF_RND71_K<K> plus() {
            return new PREF_RND71_Impl<K>(k).plus();
        }

    }

    class PREF_RND71_Impl<K> implements Api.PREF_RND71<K> {
        private final Function<? super Ast.A, K> k;

        public PREF_RND71_Impl(final Function<? super Ast.A, K> k) {
            this.k = k;
        }

        @Override
        public Api.PREF_RND71_K<K> plus() {
            return new PREF_RND71_K_Impl<>(k);
        }

    }

    class PREF_RND71_K_Impl<K> implements Api.PREF_RND71_K<K> {
        private final Function<? super Ast.A, K> k;

        public PREF_RND71_K_Impl(final Function<? super Ast.A, K> k) {
            this.k = k;
        }

        @Override
        public K y() {
            return new Partial_B_Impl<>(k).y();
        }

        @Override
        public K z() {
            return null;
        }

    }

    class Partial_B_Impl<K> extends Y_Impl<K> implements Api.Partial_B<K> {
        private final Function<? super Ast.B, K> k;

        public Partial_B_Impl(final Function<? super Ast.B, K> k) {
            // super(k.apply(new Ast.B(new OP_Impl<>(Function.identity()).$())));
            super(new Partial_OP_Impl<>(op -> k.apply(new Ast.B(op))).$());
            this.k = k;
        }

    }

    class Partial_C_Impl<K> extends Z_Impl<K> implements Api.Partial_C<K> {
        private final Function<? super Ast.C, K> k;

        public Partial_C_Impl(final Function<? super Ast.C, K> k) {
            // super(k.apply(new Ast.C(new Ast.OP())));
            super(new Partial_OP_Impl<>(op -> k.apply(new Ast.C(op))).$());
            this.k = k;
        }

    }

    class Partial_OP_Impl<K> implements Api.Partial_OP<K> {
        private final Function<? super Ast.OP, K> k;

        public Partial_OP_Impl(final Function<? super Ast.OP, K> k) {
            this.k = k;
        }

        @Override
        public K $() {
            return k.apply(new Ast.OP());
        }

    }

    class Y_Impl<K> implements Api.Y<K> {

        private final K k;

        public Y_Impl(final K k) {
            this.k = k;
        }

        @Override
        public K y() {
            return k;
        }
    }

    class Z_Impl<K> implements Api.Z<K> {

        private final K k;

        public Z_Impl(final K k) {
            this.k = k;
        }

        @Override
        public K z() {
            return k;
        }
    }

}
