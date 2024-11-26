package compojar.scratch;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import compojar.bnf.BNF;
import compojar.bnf.Rule;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;
import compojar.gen.*;
import compojar.stack.StackMachine;
import lf.LeftFactored_Api;
import lf.LeftFactored_AstNode;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static compojar.bnf.Rule.derivation;
import static compojar.bnf.Rule.selection;
import static compojar.bnf.Symbol.terminal;
import static compojar.bnf.Symbol.variable;
import static compojar.util.JavaPoet.getInnerTypeRecursively;
import static compojar.util.T2.t2;
import static compojar.util.Util.mapFromPairs;

public class LeftFactoredTest {

    @Test
    public void indirect_common_prefix() {
        var namer = new Namer("LeftFactored", "lf");

        Terminal x = terminal("x"), y = terminal("y"), mul = terminal("mul"), plus = terminal("plus");
        Variable A = variable("A"), B = variable("B"), C = variable("C"), F = variable("F"), G = variable("G"),
                F_C = variable("F_C"), O = variable("O"), O_F = variable("O_F"), O_C = variable("O_C"),
                MUL = variable("MUL"), PLUS = variable("PLUS"), Y = variable("Y");
        var bnf = new BNF(Set.of(selection(A, B, C),
                                 selection(B, F, G),
                                 derivation(F, x, mul, y),
                                 derivation(G, y),
                                 derivation(C, x, plus, y)),
                          A);

        var canonicalBNF = new BNF(Set.of(selection(A, F_C, G),
                                          derivation(F_C, x, O),
                                          selection(O, O_F, O_C),
                                          derivation(O_F, MUL, Y),
                                          derivation(O_C, PLUS, Y),
                                          derivation(MUL, mul),
                                          derivation(PLUS, plus),
                                          derivation(Y, y),
                                          derivation(G, y)),
                                  A);

        var sm = StackMachine.fromBNF(canonicalBNF);

        JavaFile ast = new AstGenerator(namer, bnf).generateJavaFile();
        // TODO ApiGenerator should produce InterfaceDescription for each interface
        JavaFile fluentApi = new ApiGenerator(namer, sm).generateJavaFile();
        Map<TypeSpec, TypeSpec> interfaceToAstNodeMap = mapFromPairs(
                List.of(t2("A", "A"),
                        t2("F_C", "A"),
                        t2("G", "G"),
                        t2("O", "A"),
                        t2("O_F", "F"),
                        t2("O_C", "C")),
                (interName, $) -> getInnerTypeRecursively(fluentApi.typeSpec, interName),
                ($, nodeName) -> getInnerTypeRecursively(ast.typeSpec, nodeName));

        // Function<String, TypeName> getAstNodeName = s -> ClassName.get("", getInnerTypeRecursively(ast.typeSpec, s).name);
        Function<String, ClassName> getAstNodeName = namer::astNodeClassName;

        Map<TypeSpec, InterfaceDescription> interfaceDescriptionMap = new HashMap<>();
        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "A"),
                                    new InterfaceDescription(getBnfRule(canonicalBNF, "A"), new ParserInfo.Full(getAstNodeName.apply("A"))));
        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "F_C"),
                                    new InterfaceDescription(getBnfRule(canonicalBNF, "F_C"), new ParserInfo.Full(getAstNodeName.apply("A"))));
        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "G"),
                                    new InterfaceDescription(getBnfRule(canonicalBNF, "G"), new ParserInfo.Full(getAstNodeName.apply("G"))));
        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "O"),
                                    new InterfaceDescription(getBnfRule(canonicalBNF, "O"), new ParserInfo.PartialS(getAstNodeName.apply("A"), Set.of())));
        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "O_F"),
                                    new InterfaceDescription(getBnfRule(canonicalBNF, "O_F"), new ParserInfo.PartialD(getAstNodeName.apply("F"), Set.of())));
        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "O_C"),
                                    new InterfaceDescription(getBnfRule(canonicalBNF, "O_C"), new ParserInfo.PartialD(getAstNodeName.apply("C"), Set.of())));
        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "MUL"),
                                    new InterfaceDescription(getBnfRule(canonicalBNF, "MUL"), ParserInfo.BRIDGE));
        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "PLUS"),
                                    new InterfaceDescription(getBnfRule(canonicalBNF, "PLUS"), ParserInfo.BRIDGE));
        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "Y"),
                                    new InterfaceDescription(getBnfRule(canonicalBNF, "Y"), ParserInfo.BRIDGE));

        final var fluentInterfaces = fluentApi.typeSpec.typeSpecs;
        final var astNodeTypes = ast.typeSpec.typeSpecs.stream()
                .collect(Collectors.toMap(ty -> namer.astNodeClassName(ty.name), Function.identity()));
        JavaFile apiImpl = new ApiImplGenerator(namer, fluentInterfaces, interfaceToAstNodeMap, interfaceDescriptionMap, astNodeTypes)
                .generateJavaFile();

        String astCode = ast.toString();
        String apiCode = fluentApi.toString();
        String apiImplCode = apiImpl.toString();

        System.out.println("// AST");
        System.out.println();
        System.out.println(astCode);
        System.out.println();
        System.out.println("// API");
        System.out.println(apiCode);
        System.out.println();
        System.out.println("// API Implementation");
        System.out.println(apiImplCode);

        Path destPath = Path.of("src/test/generated-sources/").toAbsolutePath();

        try {
            Files.createDirectories(destPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Stream.of(ast, fluentApi, apiImpl)
                .forEach(jf -> {
                    try {
                        jf.writeTo(destPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    static Rule getBnfRule(BNF bnf, CharSequence varName) {
        return bnf.rules().stream()
                .filter(rule -> rule.lhs().name().toString().contentEquals(varName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No rule for variable %s".formatted(varName)));
    }

    public static void main(String[] args) {
        var x0 = LeftFactored_Api.start().x().mul().y();
        var x1 = LeftFactored_Api.start().x().plus().y();
        var x2 = LeftFactored_Api.start().y();
    }

}
