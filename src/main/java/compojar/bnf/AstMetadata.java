package compojar.bnf;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import compojar.gen.ParserInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AstMetadata {

    static AstMetadata of(Map<Variable, ParserInfo> parserInfoMap, Collection<AstNodeMetadata> astNodeMetadatas) {
        return new AstMetadataImpl(parserInfoMap, astNodeMetadatas);
    }

    static AstMetadata empty() {
        return of(Map.of(), List.of());
    }

    Map<Variable, ParserInfo> parserInfoMap();

    default Optional<ParserInfo> getParserInfo(Variable var) {
        return Optional.ofNullable(parserInfoMap().get(var));
    }

    default ParserInfo requireParserInfo(Variable var) {
        return getParserInfo(var)
                .orElseThrow(() -> new IllegalArgumentException("No parser info for %s".formatted(var)));
    }

    AstMetadata addParserInfo(Variable variable, ParserInfo parserInfo);

    AstMetadata removeVariables(List<Variable> unused);

    AstMetadata updateParserInfos(Map<Variable, ParserInfo> newParserInfoMap);

    Optional<FieldSpec> findAstNodeField(ClassName astNodeName, Symbol symbol);

    AstMetadata addNodeMetadata(AstNodeMetadata astNodeMetadata);

    AstMetadata merge(AstMetadata astMetadata);

    Collection<AstNodeMetadata> astNodeMetadatas();

    // AstMetadata EMPTY = new AstMetadata() {
    //     @Override
    //     public Map<Variable, ParserInfo> parserInfoMap() {
    //         return Map.of();
    //     }
    //
    //     @Override
    //     public Optional<FieldSpec> findAstNodeField(final ClassName astNodeName, final Symbol symbol) {
    //         return Optional.empty();
    //     }
    //
    //     @Override
    //     public AstMetadata removeVariables(final List<Variable> unused) {
    //         return this;
    //     }
    //
    //     @Override
    //     public AstMetadata updateParserInfos(final Map<Variable, ParserInfo> newParserInfoMap) {
    //         return this;
    //     }
    // };

}
