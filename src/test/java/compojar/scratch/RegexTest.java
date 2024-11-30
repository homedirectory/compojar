package compojar.scratch;

import org.junit.Test;
import regex.Regex_AstNode;

import static regex.Regex_Api.start;

public class RegexTest {

    @Test
    public void a() {
        Regex_AstNode a = start().a().$();
        // Assert.assertEquals(new Regex_AstNode.Str(), a);
        Regex_AstNode groupOfA = start().start().a().end().$();
        Regex_AstNode optA = start().optional().a().$();
        Regex_AstNode zeroOrMoreA = start().zeroOrMore().a().$();
    }

}
