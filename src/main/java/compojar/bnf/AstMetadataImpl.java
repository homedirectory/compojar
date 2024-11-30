package compojar.bnf;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import compojar.gen.ParserInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static compojar.util.Util.*;

public record AstMetadataImpl (Map<Variable, ParserInfo> parserInfoMap,
                               Collection<AstNodeMetadata> astNodeMetadatas)
        implements AstMetadata
{

    @Override
    public AstMetadata addParserInfo(final Variable variable, final ParserInfo parserInfo) {
        return addParserInfos(Map.of(variable, parserInfo));
    }

    public AstMetadata addParserInfos(Map<Variable, ParserInfo> parserInfoMap) {
        return updateParserInfos(mapStrictMerge(this.parserInfoMap, parserInfoMap));
    }

    @Override
    public AstMetadata removeVariables(final List<Variable> variables) {
        return updateParserInfos(mapRemove(parserInfoMap, variables));
    }

    @Override
    public AstMetadata updateParserInfos(final Map<Variable, ParserInfo> newParserInfoMap) {
        return new AstMetadataImpl(newParserInfoMap, astNodeMetadatas);
    }

    @Override
    public AstMetadata updateParserInfo(Variable var, Function<? super ParserInfo, ? extends ParserInfo> fn) {
        var info = parserInfoMap.get(var);
        if (info == null)
            throw new IllegalArgumentException(String.format("No parser info for variable %s", var));
        return updateParserInfos(insert(parserInfoMap, var, fn.apply(info)));
    }

    @Override
    public Optional<FieldSpec> findAstNodeField(final ClassName astNodeName, final Symbol symbol) {
        final var astNodeMetadata = astNodeMetadatas.stream()
                .filter(x -> x.astNodeClassName().equals(astNodeName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such AST node: %s".formatted(astNodeName)));
        return Optional.ofNullable(astNodeMetadata.componentMap().get(symbol));
    }

    @Override
    public AstMetadata addNodeMetadata(final AstNodeMetadata astNodeMetadata) {
        return new AstMetadataImpl(parserInfoMap, append(astNodeMetadatas, astNodeMetadata));
    }

    @Override
    public AstMetadata merge(final AstMetadata astMetadata) {
        return new AstMetadataImpl(mapStrictMerge(parserInfoMap, astMetadata.parserInfoMap()),
                                   strictMerge(astNodeMetadatas, astMetadata.astNodeMetadatas(), (a, b) -> a.astNodeClassName().equals(b.astNodeClassName())));
    }

}
