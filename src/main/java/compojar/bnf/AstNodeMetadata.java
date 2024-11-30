package compojar.bnf;

import com.squareup.javapoet.ClassName;

/**
 * @param astNodeClassName  name of an AST node
 * @param variable  LHS of a rule that parses the AST node
 */
public record AstNodeMetadata(ClassName astNodeClassName, Variable variable) {

}
