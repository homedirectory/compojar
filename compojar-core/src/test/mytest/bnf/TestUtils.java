package compojar.bnf;

import org.junit.Assert;

public class TestUtils {

    public static void assertBnfSemanticEquals(BNF expected, BNF actual) {
        if (!expected.semanticEquals(actual)) {
            Assert.fail("""
                        BNF are semantically different.
                        
                        Expected:
                        %s
                        
                        Actual:
                        %s
                        """.formatted(expected, actual));
        }
    }

}
