package compojar.gen;

import com.squareup.javapoet.*;
import compojar.bnf.*;
import compojar.gen.ParserInfo.Bridge;
import compojar.util.*;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
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
            case Bridge bridge -> {
                // If the terminal has no parameters, declare field `k` of type `K`.
                // Otherwise, declare field `k` of type `F`, where:
                // F - a function with n parameters and return type K;
                // n - number of terminal parameters.
                var fieldType = bridge.terminal().hasParameters()
                        ? functionTypeName(bridge.terminal().getParameters().stream().map(Parameter::type).toList(), typeVar)
                        : typeVar;
                builder.addField(FieldSpec.builder(fieldType, "k", PRIVATE, FINAL).build());
            }
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

        // 2. Fields for parameters that should be supplied.
        final var fields = fieldsForParameters(interDesc);

        builder.addFields(fields);

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
            case Derivation derivation when !(interDesc.parserInfo() instanceof Bridge) -> {
                switch (derivation.rhs().getFirst()) {
                    // If the right-hand side of the rule starts with a terminal, then declare a method for that terminal.
                    case Terminal terminal -> {
                        var rhsRest = subList(derivation.rhs(), 1);
                        var astNodeType = requireAstNodeFor(inter);
                        var parameters = buildParameters(terminal.getParameters());
                        builder.addMethod(methodBuilder(terminal.name().toString())
                                                  .addModifiers(PUBLIC)
                                                  .addParameters(parameters)
                                                  .returns(parameterisedFold(rhsRest, typeVar))
                                                  .addStatement(CodeBlock.of("return $L",
                                                                             parserCode(interDesc,
                                                                                        namer.astNodeClassName(astNodeType.name),
                                                                                        rhsRest,
                                                                                        parameters.stream().map(p -> p.name).toList(),
                                                                                        List.of())))
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
        if (interDesc.parserInfo() instanceof Bridge) {
            // Implement the single method corresponding to the terminal being modelled, and the method's body is `return k`.
            if (inter.methodSpecs.size() != 1) {
                throw new IllegalStateException(
                        format("Expected bridge interface %s to have a single method, but it has %s",
                               inter.name, inter.methodSpecs.size()));
            }
            final var interMethod = inter.methodSpecs.getFirst();
            builder.addMethod(JavaPoet.methodBuilder(interMethod)
                                      .addModifiers(PUBLIC)
                                      .addStatement(interMethod.parameters.isEmpty()
                                                            ? CodeBlock.of("return this.k")
                                                            : CodeBlock.of("return this.k.apply($L)",
                                                                           interMethod.parameters.stream().map(p -> p.name).collect(joining(", "))))
                                      .build());
        }

        // Derivation with the first RHS symbol being a non-terminal needs a super() statement.
        // Terminal parameters don't make sense here, so we use an empty list.
        Optional<CodeBlock> superStatement = switch (interDesc.rule()) {
            case Derivation derivation when !(interDesc.parserInfo() instanceof Bridge) && derivation.rhs().getFirst() instanceof Variable v1 -> {
                var astNodeType = requireAstNodeFor(inter);
                // If the first RHS symbol is a bridge, the call to super either needs a lambda with 1 or more parameters
                // or doesn't need a lambda at all.
                if (interfaceDescription(interfaceForVariable(v1)).parserInfo() instanceof Bridge bridge) {
                    List<String> lambdaParams = bridge.terminal().getParameters().stream().map(p -> p.name().toString()).toList();
                    String code = lambdaParams.isEmpty()
                            ? "super($L)"
                            : "super((%s) -> $L)".formatted(String.join(", ", lambdaParams));
                    yield Optional.of(CodeBlock.of(code,
                                                   parserCode(interDesc,
                                                              namer.astNodeClassName(astNodeType.name),
                                                              // Skip the first variable as we are not instantiating it
                                                              subList(derivation.rhs(), 1),
                                                              List.of(),
                                                              lambdaParams)));
                }
                else {
                    var localVar = "x0";
                    yield Optional.of(CodeBlock.of("super($N -> $L)",
                                                   localVar,
                                                   parserCode(interDesc,
                                                              namer.astNodeClassName(astNodeType.name),
                                                              // Skip the first variable as we are not instantiating it
                                                              subList(derivation.rhs(), 1),
                                                              List.of(),
                                                              List.of(localVar))));
                }
            }
            default -> Optional.empty();
        };

        builder.addMethod(recordStyleConstructor(builder.fieldSpecs, superStatement));


        return builder.build();
    }

    private List<FieldSpec> fieldsForParameters(InterfaceDescription interDesc) {
        final List<FieldSpec> fields = switch (interDesc.parserInfo()) {
            case ParserInfo.PartialD d -> {
                // Declare fields for all parameters that should be supplied.
                final var paramsFields = d.parameters().stream()
                        .map(pair -> pair.map((ty, name) -> FieldSpec.builder(ty, name, PRIVATE, FINAL).build()))
                        .toList();
                yield paramsFields;
            }
            case ParserInfo.PartialS s -> {
                // Declare fields for all parameters that should be supplied.
                var paramsFields = enumeratedStream(s.parameters().stream(),
                                                        (c, i) -> FieldSpec.builder(c, "p" + i, PRIVATE, FINAL).build())
                        .toList();
                yield paramsFields;
            }
            default -> List.of();
        };
        return fields;
    }

    private ParameterizedTypeName functionTypeName(List<? extends Type> paramTypes, TypeName returnType) {
        if (paramTypes.isEmpty())
            throw new IllegalArgumentException("Expected non-empty [paramTypes]");

        return switch (paramTypes.size()) {
            case 0 -> throw new IllegalArgumentException("Expected non-empty [paramTypes]");
            case 1 -> ParameterizedTypeName.get(ClassName.get(Function.class), TypeName.get(paramTypes.getFirst()), returnType);
            case 2 -> ParameterizedTypeName.get(ClassName.get(BiFunction.class),
                                                TypeName.get(paramTypes.getFirst()),
                                                TypeName.get(paramTypes.get(1)),
                                                returnType);
            case 3 -> ParameterizedTypeName.get(ClassName.get(Function3.class),
                                                TypeName.get(paramTypes.getFirst()),
                                                TypeName.get(paramTypes.get(1)),
                                                TypeName.get(paramTypes.get(2)),
                                                returnType);
            case 4 -> ParameterizedTypeName.get(ClassName.get(Function4.class),
                                                TypeName.get(paramTypes.getFirst()),
                                                TypeName.get(paramTypes.get(1)),
                                                TypeName.get(paramTypes.get(2)),
                                                TypeName.get(paramTypes.get(3)),
                                                returnType);
            case 5 -> ParameterizedTypeName.get(ClassName.get(Function5.class),
                                                TypeName.get(paramTypes.getFirst()),
                                                TypeName.get(paramTypes.get(1)),
                                                TypeName.get(paramTypes.get(2)),
                                                TypeName.get(paramTypes.get(3)),
                                                TypeName.get(paramTypes.get(4)),
                                                returnType);
            default -> throw new IllegalStateException("Unsupported number of parameters for a function type: " + paramTypes.size());
        };
    }

    private List<ParameterSpec> buildParameters(List<Parameter> parameters) {
        return parameters.stream()
                .map(p -> ParameterSpec.builder(p.type(), p.name().toString()).build())
                .toList();
    }

    /**
     * Constructs a parsing expression for an interface implementation.
     *
     * @param interDesc  description of the interface being implemeneted
     * @param astNodeName  name of the AST node produced by this parser
     * @param rhs  part of the rule's RHS that represents the continuation, which implicitly ends with type variable K
     * @param parameters  names of parameters declared by the method for which the code is built
     * @param localVars  local variables introduced in the parsing expression so far
     */
    private CodeBlock parserCode(InterfaceDescription interDesc, ClassName astNodeName, List<Symbol> rhs, List<String> parameters, List<String> localVars) {
        var nextInterNames = rhs.stream()
                .map(s -> (Variable) s).map(this::interfaceForVariable)
                .map(inter -> namer.fluentInterfaceClassName(inter.name))
                .toList();
        var allParameters = concatList(fieldsForParameters(interDesc).stream().map(fs -> fs.name).toList(),
                                       parameters);
        return interDesc.parserInfo().implicitVar().map(implicitVar -> {
                    // Local variable for the implicitly parsed node needs to be prepended to the list of all local variables
                    // because the parsing order may be slightly reversed if this expression is inside super().
                    // This is because the first local variable in super() corresponds to an RHS symbol that comes after the implicit variable.
                    // For example, given the following rewritten grammar:
                    // A -> C
                    // B -> empty
                    // Here the original RHS for A was 'B C', so now A implicitly parses B.
                    // Then, the parser for A extends the parser for C, resulting in the following code:
                    /*
                    class A<K> extends C<K> {
                        Function<? super Node.A, K> k;
                        A(Function<? super Node.A, K> k) {
                            super(c -> new B<>(b -> k.apply(new Node.A(b, c))).$());
                        }
                    }
                    */
                    // Note that local variable 'c' is introduced before 'b', but in the argument list of the Node.A constructor
                    // call 'b' comes first.

                    var newLocalVar = "x" + localVars.size();
                    var code = parserCode_(interDesc,
                                           astNodeName,
                                           nextInterNames,
                                           List.of(),
                                           prepend(newLocalVar, localVars));
                    // The implicitly parsed node consumes all parameters collected so far.
                    var constructorArgs = allParameters.isEmpty()
                            ? CodeBlock.of("$L -> $L", newLocalVar, code)
                            : CodeBlock.of("$L -> $L, $L", newLocalVar, code, String.join(", ", allParameters));
                    return CodeBlock.of("new $T<>($L).$L()",
                                        implClassName(interfaceForVariable(implicitVar)),
                                        constructorArgs,
                                        namer.specialEmptyMethodName());
                })
                // The last parsed node consumes all parameters collected so far.
                .orElseGet(() -> parserCode_(interDesc,
                                             astNodeName,
                                             nextInterNames,
                                             allParameters,
                                             localVars));
    }

    private CodeBlock parserCode_(
            InterfaceDescription interDesc,
            ClassName astNodeName,
            List<ClassName> nextInterNames,
            List<String> parameters,
            List<String> localVars)
    {
        // Encountered K
        if (nextInterNames.isEmpty()) {
            return CodeBlock.of("k.apply(new $T(%s))".formatted(String.join(", ", concatList(parameters, localVars))),
                                astNodeName);
        }
        // Encountered partial parser
        else if (interfaceDescription(nextInterNames.getFirst()).parserInfo() instanceof ParserInfo.PartialS) {
            if (nextInterNames.size() != 1) {
                throw new IllegalStateException("Illegal next impl names: %s. Partial parser %s must be the last one."
                                                        .formatted(nextInterNames, nextInterNames.getFirst()));
            }
            return CodeBlock.of("new $T<>(%s)".formatted(String.join(", ", concatList(List.of("k"), parameters, localVars))),
                                implClassName(nextInterNames.getFirst()));
        }
        // Encountered full parser
        else if (interfaceDescription(nextInterNames.getFirst()).parserInfo() instanceof ParserInfo.Full) {
            final String newLocalVar = "x" + localVars.size();
            return CodeBlock.of("new $T<>(%s -> $L)".formatted(newLocalVar),
                                implClassName(nextInterNames.getFirst()),
                                parserCode_(interDesc, astNodeName, subList(nextInterNames, 1), parameters, append(localVars, newLocalVar)));
        }
        // Encountered bridge
        else if (interfaceDescription(nextInterNames.getFirst()).parserInfo() instanceof Bridge bridge) {
            // Local vars are created for each parameter of the termninal associated with the bridge.
            var newLocalVars = bridge.terminal().getParameters().stream().map(Parameter::name).map(CharSequence::toString).toList();
            String code = bridge.terminal().hasParameters()
                    ? "new $T<>((%s) -> $L)".formatted(String.join(", ", newLocalVars))
                    : "new $T<>($L)";
            return CodeBlock.of(code,
                                implClassName(nextInterNames.getFirst()),
                                parserCode_(interDesc, astNodeName, subList(nextInterNames, 1), parameters, concatList(localVars, newLocalVars)));
        }
        else {
            throw new IllegalStateException("Illegal parser continuation: %s.".formatted(nextInterNames));
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
