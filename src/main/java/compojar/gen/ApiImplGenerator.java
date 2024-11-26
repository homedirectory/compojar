package compojar.gen;

import com.squareup.javapoet.*;
import compojar.bnf.*;
import compojar.util.JavaPoet;
import compojar.util.T2;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static compojar.gen.ParserInfo.BRIDGE;
import static compojar.util.JavaPoet.recordStyleConstructor;
import static compojar.util.T2.t2;
import static compojar.util.Util.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.*;
import static javax.lang.model.element.Modifier.*;

/**
 * <li> Type arguments of fluent interface implementation types are always the types of fluent interfaces instead of the
 * types of their implementations. This is due to type limited subtyping of generic types in Java.
 */
public class ApiImplGenerator {

    private final Namer namer;
    private final Map<ClassName, TypeSpec> astNodes;
    private final Map<ClassName, TypeSpec> fluentInterfacesMap;
    // one-to-one even for selection rules, for which the common AST node type is used
    private final Map<TypeSpec, TypeSpec> fluentInterfaceToAstNodeMap;
    private final Map<TypeSpec, InterfaceDescription> interfaceDescriptionMap;

    public ApiImplGenerator(
            Namer namer,
            Collection<TypeSpec> fluentInterfaces,
            Map<TypeSpec, TypeSpec> fluentInterfaceToAstNodeMap,
            Map<TypeSpec, InterfaceDescription> interfaceDescriptionMap,
            Map<ClassName, TypeSpec> astNodes)
    {
        this.namer = namer;
        this.astNodes = astNodes;
        this.fluentInterfacesMap = fluentInterfaces.stream()
                .collect(toMap(ty -> namer.fluentInterfaceClassName(ty.name), Function.identity()));
        this.fluentInterfaceToAstNodeMap = fluentInterfaceToAstNodeMap;
        this.interfaceDescriptionMap = interfaceDescriptionMap;
    }

    public String generateCode() {
        return generateJavaFile().toString();
    }

    private Collection<TypeSpec> fluentInterfaces() {
        return fluentInterfacesMap.values();
    }

    private TypeSpec requireAstNodeFor(TypeSpec fluentInterface) {
        return requireKey(fluentInterfaceToAstNodeMap, fluentInterface,
                          k -> "Missing AST node type for fluent interface %s".formatted(fluentInterface.name));
    }

    private TypeSpec requireAstNode(ClassName typeName) {
        return requireKey(astNodes, typeName, $ -> "No such AST node: %s".formatted(typeName));
    }

    private Optional<TypeSpec> astNodeFor(TypeSpec inter) {
        return Optional.ofNullable(fluentInterfaceToAstNodeMap.get(inter));
    }

    private InterfaceDescription interfaceDescription(TypeSpec inter) {
        return requireKey(interfaceDescriptionMap, inter,
                          k -> "Missing interface description for fluent interface %s".formatted(inter.name));
    }

    private InterfaceDescription interfaceDescription(ClassName interName) {
        // TODO could have a separate map for AST nodes
        return interfaceDescriptionMap.entrySet().stream()
                .filter(entry -> interName.simpleName().equals(entry.getKey().name))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new IllegalStateException("No interface description for %s".formatted(interName)));
    }

    private TypeSpec interfaceForVariable(Variable variable) {
        // TODO bi-directional map
        return interfaceDescriptionMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().rule().lhs().equals(variable))
                .findAny()
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalStateException("No fluent interface for variable %s".formatted(variable)));
    }

    private Variable variableForInterface(TypeSpec inter) {
        return interfaceDescription(inter).rule().lhs();
    }

    public JavaFile generateJavaFile() {
        var topLevelTypeBuilder = interfaceBuilder(namer.enclosingApiImplTypeClassName());

        var implementations = fluentInterfaces().stream()
                .map(this::genImpl)
                .map(impl -> impl.toBuilder().addModifiers(PUBLIC, STATIC).build())
                .collect(toSet());

        topLevelTypeBuilder.addTypes(implementations);

        return JavaFile.builder(namer.pkgName(), topLevelTypeBuilder.build()).build();
    }

    private TypeSpec genImpl(TypeSpec inter) {
        var builder = classBuilder(implClassName(inter.name));

        var typeVar = TypeVariableName.get("K");
        builder.addTypeVariable(typeVar);

        builder.addSuperinterface(ParameterizedTypeName.get(namer.fluentInterfaceClassName(inter.name), typeVar));

        var interDesc = interfaceDescription(inter);
        // Declare all necesary fields.
        // 1. Field for the continuation.
        switch (interDesc.parserInfo()) {
            case ParserInfo.Bridge $ ->
                // Declare field `k` of type `K`.
                    builder.addField(FieldSpec.builder(typeVar, "k", PRIVATE, FINAL).build());
            default -> {
                // Declare field `k` of type `Function<? super Node, K>` where `Node` is the most specific AST node parsed by the fluent interface.
                var astNodeType = requireAstNodeFor(inter);
                builder.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Function.class),
                                                                             WildcardTypeName.supertypeOf(namer.astNodeClassName(astNodeType.name)),
                                                                             typeVar),
                                                   "k",
                                                   PRIVATE, FINAL)
                                         .build());
            }
        }

        // 2. Fields for components that should be supplied.
        final List<FieldSpec> componentFields = switch (interDesc.parserInfo()) {
            case ParserInfo.PartialD (var astNodeName, var componentNames) -> {
                final var astNode = requireAstNode(astNodeName);
                // Declare fields for all components of the corresponding AST node that should be supplied.
                yield componentNames.stream()
                        .map(c -> {
                            final var fieldSpec = JavaPoet.getField(astNode, c);
                            return FieldSpec.builder(fieldSpec.type, c, PRIVATE, FINAL).build();
                        })
                        .toList();
            }
            case ParserInfo.PartialEmpty (var astNodeName) -> {
                yield List.of(FieldSpec.builder(astNodeName, namer.emptyParserFieldName(), PRIVATE, FINAL).build());
            }
            case ParserInfo.PartialS (var astNodeName, var componentTypeNames) -> {
                // Declare fields for all components of the corresponding AST node that should be supplied.
                yield enumeratedStream(componentTypeNames.stream(),
                                       (c, i) -> FieldSpec.builder(c, "c" + i, PRIVATE, FINAL).build())
                        .toList();
            }
            default -> List.of();
        };

        builder.addFields(componentFields);

        // FIXME why are repeating the logic of API generation but with a BNF instead of a stack machine?
        //       we need to just follow the interface definition
        switch (interDesc.rule()) {
            case Selection selection -> {
                // Implement methods of all options via delegation.
                // This relies on the correct order of fields declared in this implementation type: zero or more components followed by a continuation.
                final List<String> argumentsToDelegateCtors = builder.fieldSpecs.stream().map(f -> f.name).toList();
                inter.superinterfaces.stream()
                        .map(this::getFluentInterface)
                        .flatMap(superInter -> allApiMethods(superInter).map(m -> buildDelegatingMethod(m, argumentsToDelegateCtors, implClassName(superInter))))
                        .forEach(builder::addMethod);
            }
            case Derivation derivation when interDesc.parserInfo() != BRIDGE -> {
                switch (derivation.rhs().getFirst()) {
                    // If the right-hand side of the rule starts with a terminal, then declare a method for that terminal.
                    case Terminal terminal -> {
                        var astNodeType = requireAstNodeFor(inter);
                        builder.addMethod(methodBuilder(terminal.name().toString())
                                                  .addModifiers(PUBLIC)
                                                  .returns(parameterisedFold(subList(derivation.rhs(), 1), typeVar))
                                                  .addStatement(CodeBlock.of("return $L",
                                                                             parserCode(interDesc,
                                                                                        namer.astNodeClassName(astNodeType.name),
                                                                                        componentFields.stream().map(f -> f.name).toList(),
                                                                                        subList(derivation.rhs(), 1))))
                                                  .build());
                    }
                    // Otherwise, extend C_j, where C_j is the implementation type corresponding to the first symbol in the right-hand side.
                    case Variable variable -> {
                        builder.superclass(parameterisedFold(derivation.rhs(), typeVar));
                    }
                }
            }
            default -> {}
        }

        // Handle the case of a bridge parser.
        if (interDesc.parserInfo() instanceof ParserInfo.Bridge) {
            // Implement the single method corresponding to the terminal being modelled, and the method's body is `return k`.
            if (inter.methodSpecs.size() != 1) {
                throw new IllegalStateException(
                        format("Expected bridge interface %s to have a single method, but it has %s",
                               inter.name, inter.methodSpecs.size()));
            }
            final var interMethod = inter.methodSpecs.getFirst();
            builder.addMethod(JavaPoet.methodBuilder(interMethod)
                                      .addModifiers(PUBLIC)
                                      .addStatement("return this.k")
                                      .build());
        }

        // Derivation with the first RHS symbol being a non-terminal needs a super() statement.
        Optional<CodeBlock> superStatement = switch (interDesc.rule()) {
            case Derivation derivation when interDesc.parserInfo() != BRIDGE && derivation.rhs().getFirst() instanceof Variable v1 -> {
                var astNodeType = requireAstNodeFor(inter);
                // If the first RHS symbol is a bridge, the call to super does not need a lambda.
                if (interfaceDescription(interfaceForVariable(v1)).parserInfo() instanceof ParserInfo.Bridge) {
                    yield Optional.of(CodeBlock.of("super($L)",
                                                   parserCode(interDesc,
                                                              namer.astNodeClassName(astNodeType.name),
                                                              componentFields.stream().map(f -> f.name).toList(),
                                                              // Skip the first variable as we are not instantiating it
                                                              subList(derivation.rhs(), 1),
                                                              List.of())));
                }
                else {
                    var localVar = "x0";
                    yield Optional.of(CodeBlock.of("super($N -> $L)",
                                                   localVar,
                                                   parserCode(interDesc,
                                                              namer.astNodeClassName(astNodeType.name),
                                                              componentFields.stream().map(f -> f.name).toList(),
                                                              // Skip the first variable as we are not instantiating it
                                                              subList(derivation.rhs(), 1),
                                                              List.of(localVar))));
                }
            }
            default -> Optional.empty();
        };

        builder.addMethod(recordStyleConstructor(builder.fieldSpecs, superStatement));


        return builder.build();
    }

    private CodeBlock parserCode(InterfaceDescription interDesc, ClassName astNodeName, List<String> componentFieldNames, List<Symbol> rhs) {
        return parserCode(interDesc, astNodeName, componentFieldNames, rhs, List.of());
    }

    private CodeBlock parserCode(InterfaceDescription interDesc, ClassName astNodeName, List<String> componentFieldNames, List<Symbol> rhs, List<String> localVars) {
        return parserCode_(interDesc,
                           astNodeName,
                           componentFieldNames,
                           rhs.stream().map(s -> (Variable) s).map(this::interfaceForVariable).map(inter -> namer.fluentInterfaceClassName(inter.name)).toList(),
                           localVars);
    }

    private CodeBlock parserCode_(
            InterfaceDescription interDesc,
            ClassName astNodeName, List<String> componentFieldNames,
            List<ClassName> nextInterNames, List<String> localVars)
    {
        // Encountered K
        if (nextInterNames.isEmpty()) {
            return switch (interDesc.parserInfo()) {
                case ParserInfo.PartialEmpty it -> CodeBlock.of("k.apply($L)", namer.emptyParserFieldName());
                default -> CodeBlock.of("k.apply(new $T(%s))".formatted(String.join(", ", concatList(componentFieldNames, localVars))),
                                        astNodeName);
            };
        }
        // Encountered partial parser
        else if (interfaceDescription(nextInterNames.getFirst()).parserInfo() instanceof ParserInfo.PartialS) {
            if (nextInterNames.size() != 1) {
                throw new IllegalStateException("Illegal next impl names: %s. Partial parser %s must be the last one."
                                                        .formatted(nextInterNames, nextInterNames.getFirst()));
            }
            return CodeBlock.of("new $T<>(%s)".formatted(String.join(", ", concatList(List.of("k"), componentFieldNames, localVars))),
                                implClassName(nextInterNames.getFirst()));
        }
        // Encountered full parser
        else if (interfaceDescription(nextInterNames.getFirst()).parserInfo() instanceof ParserInfo.Full) {
            final String newLocalVar = "x" + localVars.size();
            return CodeBlock.of("new $T<>(%s -> $L)".formatted(newLocalVar),
                                implClassName(nextInterNames.getFirst()),
                                parserCode_(interDesc, astNodeName, componentFieldNames, subList(nextInterNames, 1), append(localVars, newLocalVar)));
        }
        // Encountered partial empty parser
        else if (interfaceDescription(nextInterNames.getFirst()).parserInfo() instanceof ParserInfo.PartialEmpty) {
            return CodeBlock.of("k.apply(%L)",
                                namer.emptyParserFieldName());
        }
        // Encountered bridge
        else if (interfaceDescription(nextInterNames.getFirst()).parserInfo() instanceof ParserInfo.Bridge) {
            // Same as full parser but without creating a new local var
            return CodeBlock.of("new $T<>($L)",
                                implClassName(nextInterNames.getFirst()),
                                parserCode_(interDesc, astNodeName, componentFieldNames, subList(nextInterNames, 1), localVars));
        }
        else {
            throw new IllegalStateException("Illegal next impl names: %s.".formatted(nextInterNames));
        }
    }

    private TypeName parameterisedFold(final List<Symbol> symbols, final TypeName lastTypeName) {
        if (symbols.isEmpty()) {
            return lastTypeName;
        }
        else {
            // Implementation type for the first type
            final Stream<TypeName> first = Stream.of(implClassName(interfaceForVariable((Variable) symbols.getFirst())));
            // Interface types for the rest
            final Stream<TypeName> mid = subList(symbols, 1).stream()
                    .map(sym -> (Variable) sym)
                    .map(this::interfaceForVariable)
                    .map(inter -> namer.fluentInterfaceClassName(inter.name));
            final Stream<TypeName> last = Stream.of(lastTypeName);
            return parameterisedFold(Stream.of(first, mid, last).flatMap(Function.identity()).toList());
        }
    }

    private static TypeName parameterisedFold(final List<TypeName> typeNames) {
        if (typeNames.isEmpty()) throw new IllegalArgumentException("empty list");

        return typeNames.size() == 1
                ? typeNames.getFirst()
                : ParameterizedTypeName.get((ClassName) typeNames.getFirst(), parameterisedFold(subList(typeNames, 1)));
    }

    private MethodSpec buildDelegatingMethod(final MethodSpec apiMethod, final List<String> argumentsToDelegateCtor, final ClassName delegateClassName) {
        return methodBuilder(apiMethod.name)
                .addModifiers(PUBLIC)
                .addParameters(apiMethod.parameters)
                .addStatement("return new $T<>($L).$L($L)",
                              delegateClassName,
                              String.join(", ", argumentsToDelegateCtor),
                              apiMethod.name,
                              apiMethod.parameters.stream().map(p -> p.name).collect(joining(", ")))
                .returns(apiMethod.returnType)
                .build();
    }

    private TypeSpec getFluentInterface(TypeName typeName) {
        final var inter = switch (typeName) {
            case ClassName it -> fluentInterfacesMap.get(it);
            case ParameterizedTypeName it -> fluentInterfacesMap.get(it.rawType);
            default -> throw new IllegalStateException("Unexpected value: " + typeName);
        };
        Objects.requireNonNull(inter, () -> "No fluent interface for name: %s".formatted(typeName));
        return inter;
    }

    /**
     * Returns all API methods reachable from the specified interface.
     * All type variables in returned methods are substituted accordingly.
     */
    private Stream<MethodSpec> allApiMethods(TypeSpec fluentInterface) {
        // TODO distinguish API methods (e.g., by annotation)
        return Stream.concat(Stream.of(T2.<TypeSpec, Map<String, TypeName>>t2(fluentInterface, Map.of())),
                             subsSuperInterfaces(fluentInterface, Map.of()))
                .distinct()
                .flatMap(pair -> pair.map((superInter, subs) -> superInter.methodSpecs.stream().map(m -> JavaPoet.substTypes(m, subs))));
    }

    /**
     * Returns a stream of superinterfaces of the specified type accompanied by type substitutions that arise from their usage.
     *
     * @param subs  substitutions to use
     */
    private Stream<T2<TypeSpec, Map<String, TypeName>>> subsSuperInterfaces(TypeSpec currType, Map<String, TypeName> subs) {
        return currType.superinterfaces.stream()
                .map(superInterName -> {
                    final var superInter = getFluentInterface(superInterName);
                    final var superSubs = JavaPoet.getSubsts(superInter, superInterName, subs);
                    return t2(superInter, superSubs);
                })
                .flatMap(pair -> Stream.concat(Stream.of(pair), pair.map((x, y) -> subsSuperInterfaces(x, y))));
    }

    private ClassName implClassName(CharSequence interfaceName) {
        return namer.fluentInterfaceImplClassName(namer.implSimpleName(interfaceName));
    }

    private ClassName implClassName(ClassName interfaceName) {
        return implClassName(interfaceName.simpleName());
    }

    private ClassName implClassName(TypeSpec interfaceType) {
        return implClassName(interfaceType.name);
    }

}
