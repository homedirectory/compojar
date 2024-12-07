package compojar.scratch;

import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;
import compojar.gen.Generator;
import compojar.gen.Namer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;

import static compojar.bnf.Rule.derivation;
import static compojar.bnf.Rule.selection;
import static compojar.scratch.LeftFactored2.T.*;
import static compojar.scratch.LeftFactored2.V.*;

public class LeftFactored2 {

    public static void main(String[] args)
            throws IOException
    {
        var namer = new Namer("LeftFactored2", "left_factored2");

        Path destPath = Path.of("src/test/generated-sources/").toAbsolutePath();
        new Generator(namer, bnf).generate(destPath);
    }

    enum V implements Variable {
        A, B, C, D, OP, Y, Z,
    }

    enum T implements Terminal {
        y, z, op, b, c
    }

    static final BNF bnf = new BNF(
            Set.of(selection(A, B, C),
                   derivation(B, D, b),
                   derivation(C, D, c, c, c),
                   derivation(D, OP),
                   derivation(Y, y),
                   derivation(Z, z),
                   derivation(OP, op.parameters(String.class, "op"))),
            A);

    // <A> ::= B | C
    // B ::= D y
    // C ::= D z
    // D ::= OP
    // OP ::= plus

    interface Ast {
        interface A {}
        record B (D op) implements A {}
        record C (D op) implements A {}
        record D (OP op) {}
        record OP() {}
    }

    // <A> ::= <PREF_RND71>
    // PREF_RND71 ::= plus <PREF_RND71_K>
    // <PREF_RND71_K> ::= <Partial_B> | <Partial_C>
    // Partial_B ::= <Partial_D> y
    // Partial_C ::= <Partial_D> z
    // Partial_D ::= <Partial_OP>
    // Partial_OP ::= <empty>

    // Inline empty

    // <A> ::= <PREF_RND71>
    // PREF_RND71 ::= plus <PREF_RND71_K>
    // <PREF_RND71_K> ::= <Partial_B> | <Partial_C>
    // Partial_B ::= <Partial_D> y
    // Partial_C ::= <Partial_D> z
    // Partial_D ::= <empty> # Implicit Partial_OP
    // Partial_OP ::= <empty>

    // Inline empty

    // <A> ::= <PREF_RND71>
    // PREF_RND71 ::= plus <PREF_RND71_K>
    // <PREF_RND71_K> ::= <Partial_B> | <Partial_C>
    // Partial_B ::= y # Implicit Partial_D
    // Partial_C ::= z # Implicit Partial_D
    // Partial_D ::= <empty> # Implicit Partial_OP
    // Partial_OP ::= <empty>

    interface Api {

        interface A<K> extends PREF_RND71<K> {}

        interface PREF_RND71<K> {
            PREF_RND71_K<K> plus();
        }

        interface PREF_RND71_K<K> extends Partial_B<K>, Partial_C<K>
        {}

        interface Partial_B<K> extends Y<K> {}

        interface Partial_C<K> extends Z<K> {}

        interface Partial_D<K> {
            K $();
        }

        interface Partial_OP<K> {
            K $();
        }

        interface Y<K> {
            K y();
        }
        interface Z<K> {
            K z();
        }

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
            super(new Partial_D_Impl<>(d -> k.apply(new Ast.B(d))).$());
            this.k = k;
        }

    }

    class Partial_C_Impl<K> extends Z_Impl<K> implements Api.Partial_C<K> {
        private final Function<? super Ast.C, K> k;

        public Partial_C_Impl(final Ast.OP op, final Function<? super Ast.C, K> k) {
            super(new Partial_D_Impl<>(d -> k.apply(new Ast.C(d))).$());
            this.k = k;
        }

    }

    class Partial_D_Impl<K> implements Api.Partial_D<K> {
        private final Function<? super Ast.D, K> k;

        public Partial_D_Impl(final Function<? super Ast.D, K> k) {
            this.k = k;
        }

        @Override
        public K $() {
            return new Partial_OP_Impl<>(op -> k.apply(new Ast.D(op))).$();
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
