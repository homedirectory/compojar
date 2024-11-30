package compojar.scratch;

import org.junit.Test;
import regex.Regex_AstNode;

import static regex.Regex_Api.start;

public class RegexTest {

    @Test
    public void a() {
        Regex_AstNode a = start().str().$();
        // Assert.assertEquals(new Regex_AstNode.Str(), str);
        Regex_AstNode groupOfA = start().begin().str().end().$();
        Regex_AstNode optA = start().optional().str().$();
        Regex_AstNode zeroOrMoreA = start().zeroOrMore().str().$();
    }

}
