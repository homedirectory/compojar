package regex;

import static regex.Regex_Api.start;

public class RegexTest {

    public static void main(String[] args) {
        // ISBN-13 without hyphens
        start().times(13).digit().$();
        // IPv4 address
        start().atLeast(1).atMost(3).digit()
                .str(".")
                .atLeast(1).atMost(3).digit()
                .str(".")
                .atLeast(1).atMost(3).digit()
                .str(".")
                .atLeast(1).atMost(3).digit()
                .$();
        // Social Security Number followed by an optional extension
        // \d{3}-\d{2}-\d{4}\s*(ext\.?\s*\d{1,5})?
        start().times(3).digit()
                .str("-")
                .times(2).digit()
                .str("-")
                .times(4).digit()
                .zeroOrMore().space()
                .optional()
                .begin()
                    .str("ext")
                    .optional().str(".")
                    .zeroOrMore().space()
                    .atLeast(1).atMost(5).digit()
                    .$()
                .end()
                .$();
        // Email address
        // ([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+)\.([a-zA-Z]{2,})

        start().begin()
                    .oneOrMore().character("a-ZA-Z0-9._%+-")
                    .str("@")
                    .begin()
                        .oneOrMore().character("a-zA-Z0-9.-")
                        .$()
                    .end()
                    .str(".")
                    .begin()
                        .atLeast(2)
                        .character("a-zA-Z")
                        .$()
                    .end()
                .$()
                .end()
                .$();

    }
}
