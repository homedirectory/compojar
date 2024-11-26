package compojar.scratch;

import org.junit.Test;
import regex.Regex_AstNode.Term;

import static regex.Regex_Api.start;

public class RegexTest {

    @Test
    public void a() {
        Term a = start().a();
        // Assert.assertEquals(new Regex_AstNode.Str(), a);
        Term groupOfA = start().start().a().end();
        Term optA = start().optional().a();
        Term zeroOrMoreA = start().zeroOrMore().a();
    }

}
