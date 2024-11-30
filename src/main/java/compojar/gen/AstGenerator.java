package compojar.gen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import compojar.bnf.*;
import compojar.util.JavaPoet;
import compojar.util.T2;

import java.util.function.Function;
import java.util.stream.Stream;

import static compojar.util.JavaPoet.interfaceBuilder;
import static compojar.util.T2.t2;
import static compojar.util.Util.decapitalise;
import static compojar.util.Util.enumeratedStream;
import static java.util.stream.Collectors.toMap;
import static javax.lang.model.element.Modifier.*;

// TODO: inline intermediate nodes
public class AstGenerator {

    private final Namer namer;
    private final BNF bnf;

    public AstGenerator(final Namer namer, final BNF bnf) {
        this.namer = namer;
        this.bnf = bnf;
    }

    public String generateCode() {
        return generateJavaFile().toString();
    }

    public T2<JavaFile, AstMetadata> generate() {
        final var astNodeTypeName = namer.enclosingAstTypeClassName();
        var astNodeTypeBuilder = interfaceBuilder(astNodeTypeName);

        var astMetadata = bnf.rules().stream()
                .map(rule -> buildNode(rule, bnf, AstMetadata.empty()))
                .map(pair -> pair.map1(b -> b.addSuperinterface(astNodeTypeName).addModifiers(PUBLIC, STATIC).build()))
                .peek(pair -> astNodeTypeBuilder.addType(pair.fst()))
                .map(T2::snd)
                .reduce(AstMetadata.empty(), AstMetadata::merge);

        astMetadata = astMetadata.updateParserInfos(
                astMetadata.astNodeMetadatas().stream()
                        .collect(toMap(AstNodeMetadata::variable,
                                       astNodeMetadata -> new ParserInfo.Full(astNodeMetadata.astNodeClassName()))));

        return t2(JavaFile.builder(namer.pkgName(), astNodeTypeBuilder.build()).build(), astMetadata);
    }

    @Deprecated
    public JavaFile generateJavaFile() {
        return generate().fst();
    }

    private T2<TypeSpec.Builder, AstMetadata> buildNode(Rule rule, BNF bnf, AstMetadata astMetadata) {
        return switch (rule) {
            case Derivation derivation -> buildNodeForDerivation(derivation).map2(astMetadata::addNodeMetadata);
            case Selection selection -> buildNodeForSelection(astMetadata, selection);
        };
    }

    private T2<TypeSpec.Builder, AstMetadata> buildNodeForSelection(
            final AstMetadata astMetadata,
            final Selection selection)
    {
        final var interfaceTypeName = classNameForNode(selection.lhs().name());
        return t2(interfaceBuilder(interfaceTypeName)
                          .addSuperinterfaces(selectorsFor(selection.lhs()).map(this::classNameForNode).toList()),
                  astMetadata.addNodeMetadata(new AstNodeMetadata(interfaceTypeName, selection.lhs())));
    }

    private T2<TypeSpec.Builder, AstNodeMetadata> buildNodeForDerivation(final Derivation derivation) {
        final var nodeClassName = classNameForNode(derivation.lhs().name());
        final var builder = JavaPoet.recordBuilder(
                        nodeClassName,
                        enumeratedStream(derivation.rhs().stream(),
                                         (v, i) -> switch (v) {
                                             // TODO ensure there are no name conflicts
                                             case Terminal terminal when terminal.hasParameters() ->
                                                     terminal.getParameters().stream()
                                                             .map(param -> FieldSpec.builder(param.type(), param.name().toString(), PRIVATE, FINAL).build());
                                             case Variable variable ->
                                                     Stream.of(FieldSpec.builder(classNameForNode(variable), decapitalise(variable.name())).build());
                                             default -> Stream.<FieldSpec>of();
                                         })
                                .flatMap(Function.identity())
                                .toList())
                .addSuperinterfaces(selectorsFor(derivation.lhs())
                                            .map(this::classNameForNode)
                                            .toList());
        return t2(builder, new AstNodeMetadata(nodeClassName, derivation.lhs()));
    }

    private Stream<Variable> selectorsFor(Variable var) {
        return bnf.rules().stream()
                .filter(rule -> rule instanceof Selection s && s.rhs().contains(var))
                .map(Rule::lhs);
    }

    private ClassName classNameForNode(CharSequence v) {
        return namer.astNodeClassName(v);
    }

}
