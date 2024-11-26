package compojar.bnf;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;

import java.util.Map;

public record AstNodeMetadata(ClassName astNodeClassName, Variable variable, Map<Symbol, FieldSpec> componentMap) {

}
