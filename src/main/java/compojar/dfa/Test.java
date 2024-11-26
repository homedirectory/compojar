package compojar.dfa;

import java.util.Set;

public class Test {

    public static void main(String[] args) {
        // any number of 'a' followed by one or more 'b'
        //    b
        // S ---> O ---
        // | k----^  b \
        // v / b   \---/
        // A <-\
        // | a |
        // \---/
        var dfa = new DFA(
                // states
                Set.of("S", "A", "O"),
                // symbols
                Set.of("a", "b"),
                // rules
                Set.of(new Rule("S", "b", "O"),
                       new Rule("O", "b", "O"),
                       new Rule("S", "a", "A"),
                       new Rule("A", "a", "A"),
                       new Rule("A", "b", "O")
                ),
                // accepting states
                Set.of("O"),
                // start
                "S"
        );

        final var generator = new Generator();
        generator.generateApi("AB", dfa);
    }

    static void testAB() {
        AB.AB().a().a().a().a().b().$();
        AB.AB().a().a().a().a().b().b().b().b().$();
        AB.AB().b().$();
        AB.AB().b().b().b().$();
    }

    interface AB {
        static S AB() {
            throw new UnsupportedOperationException();
        }

        interface $End {
            void $();
        }

        interface O extends $End {
            O b();
        }

        interface A {
            O b();

            A a();
        }

        interface S {
            O b();

            A a();
        }
    }



}
