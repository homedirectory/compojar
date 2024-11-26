package compojar.gen;

import com.squareup.javapoet.*;
import compojar.stack.Rule;
import compojar.stack.StackMachine;
import compojar.stack.Symbol;
import compojar.util.T2;
import compojar.util.Util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static compojar.stack.Symbol.empty;
import static compojar.stack.Symbol.symbol;
import static compojar.util.T2.t2;
import static javax.lang.model.element.Modifier.*;

public class ApiGenerator {

    private static final TypeVariableName K = TypeVariableName.get("K");

    private final StackMachine machine;
    private final Namer namer;

    public ApiGenerator(final Namer namer, final StackMachine machine) {
        this.namer = namer;
        this.machine = machine;
    }

    public T2<JavaFile, Map<Symbol, TypeSpec>> generate() {
        var apiTypeBuilder = TypeSpec.interfaceBuilder(namer.enclosingApiTypeClassName())
                .addModifiers(PUBLIC);

        final Map<Symbol, TypeSpec> symbolInterfaceMap = machine.stackSymbols().stream()
                .collect(Collectors.toMap(Function.identity(), sym -> generateType(sym, machine.rulesThatPop(sym))));

        apiTypeBuilder.addTypes(symbolInterfaceMap.values());

        apiTypeBuilder.addMethod(
                methodBuilder("start")
                        .returns(ParameterizedTypeName.get(makeInterfaceName(machine.start()), namer.astNodeClassName(machine.start())))
                        .addModifiers(PUBLIC, STATIC)
                        .addStatement("return new $T<>($T.identity())",
                                      namer.fluentInterfaceImplClassName(namer.implSimpleName(makeInterfaceName(machine.start()).simpleName())),
                                      ClassName.get(Function.class))
                        .build());

        var apiType = apiTypeBuilder.build();
        return t2(JavaFile.builder(namer.pkgName(), apiType).skipJavaLangImports(true).build(),
                  symbolInterfaceMap);
    }

    public String generateCode() {
        return generateJavaFile().toString();
    }

    public void generateApi(Appendable sink) {
        try {
            generateJavaFile().writeTo(sink);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JavaFile generateJavaFile() {
        var apiTypeBuilder = TypeSpec.interfaceBuilder(namer.enclosingApiTypeClassName())
                .addModifiers(PUBLIC);

        machine.stackSymbols().stream()
                .map(sym -> generateType(sym, machine.rulesThatPop(sym)))
                .forEach(apiTypeBuilder::addType);

        apiTypeBuilder.addMethod(
                methodBuilder("start")
                        .returns(ParameterizedTypeName.get(makeInterfaceName(machine.start()), namer.astNodeClassName(machine.start())))
                        .addModifiers(PUBLIC, STATIC)
                        .addStatement("return new $T<>($T.identity())",
                                      namer.fluentInterfaceImplClassName(namer.implSimpleName(makeInterfaceName(machine.start()).simpleName())),
                                      ClassName.get(Function.class))
                        .build());

        var apiType = apiTypeBuilder.build();
        return JavaFile.builder(namer.pkgName(), apiType).skipJavaLangImports(true).build();
    }

    private TypeSpec generateType(Symbol symbol, Set<Rule> rules) {
        rules = adaptRules(rules);
        var builder = TypeSpec.interfaceBuilder(makeInterfaceName(symbol))
                .addModifiers(PUBLIC, STATIC)
                .addTypeVariable(K);

        // superinterface for each empty reads

        rules.stream()
                .filter(rule -> rule.reads() == empty)
                .forEach(rule -> builder.addSuperinterface(makeParameterisedType(rule.pushes())));

        // method for each non-empty reads

        rules.stream()
                .filter(rule -> rule.reads() != empty)
                .forEach(rule -> builder.addMethod(
                        methodBuilder(rule.reads().toString())
                                .returns(rule.pushes().isEmpty() ? K : makeParameterisedType(rule.pushes()))
                                .addModifiers(PUBLIC, ABSTRACT)
                                .build()));

        return builder.build();
    }

    private Set<Rule> adaptRules(final Set<Rule> rules) {
        return rules.stream()
                .map(rule -> rule.reads() == empty && rule.pushes().isEmpty()
                        ? new Rule(END_SYMBOL, rule.pops(), rule.pushes())
                        : rule)
                .collect(Collectors.toSet());
    }

    private TypeName makeParameterisedType(final List<Symbol> symbols) {
        return parameterisedFold(Util.append(symbols.stream().map(this::makeInterfaceName).toList(), K));
    }

    private TypeName parameterisedFold(final List<? extends TypeName> types) {
        if (types.isEmpty()) throw new IllegalArgumentException("empty list");

        return types.size() == 1
                ? types.getFirst()
                : ParameterizedTypeName.get((ClassName) types.getFirst(), parameterisedFold(types.subList(1, types.size())));
    }


    private ClassName makeInterfaceName(CharSequence simpleName) {
        return namer.fluentInterfaceClassName(simpleName);
    }

    public static final Symbol END_SYMBOL = symbol("$");

}
