//package compojar.scratch;
//
//import com.squareup.javapoet.ClassName;
//import com.squareup.javapoet.JavaFile;
//import com.squareup.javapoet.TypeName;
//import com.squareup.javapoet.TypeSpec;
//import compojar.bnf.BNF;
//import compojar.bnf.Rule;
//import compojar.gen.*;
//import compojar.stack.StackMachine;
//import org.junit.Test;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import static compojar.util.JavaPoet.getInnerTypeRecursively;
//import static compojar.util.T2.t2;
//import static compojar.util.Util.mapFromPairs;
//
//public class MathApiTest {
//
//    @Test
//    public void genFromCanonicalBnf() {
//        var namer = new Namer("Math", "math");
//        var bnf = Math2.originalBNF();
//        var canonicalBNF = Math2.canonicalBNF();
//        // var t = new BnfTransformer();
//        var sm = StackMachine.fromBNF(canonicalBNF);
//
//        JavaFile ast = new AstGenerator(namer, bnf).generateJavaFile();
//        // TODO ApiGenerator should produce InterfaceDescription for each interface
//        JavaFile fluentApi = new ApiGenerator(namer, sm).generateJavaFile();
//        Map<TypeSpec, TypeSpec> interfaceToAstNodeMap = mapFromPairs(
//                List.of(t2("E", "E"),
//                        t2("Prod_Sum", "E"),
//                        t2("O", "E"),
//                        t2("O1", "Sum"),
//                        t2("O2", "Prod"),
//                        t2("NegExpr", "NegExpr"),
//                        t2("N", "N"),
//                        t2("N1", "N1"),
//                        t2("N2", "N2")),
//                (interName, $) -> getInnerTypeRecursively(fluentApi.typeSpec, interName),
//                ($, nodeName) -> getInnerTypeRecursively(ast.typeSpec, nodeName));
//
//        // Function<String, TypeName> getAstNodeName = s -> ClassName.get("", getInnerTypeRecursively(ast.typeSpec, s).name);
//        Function<String, ClassName> getAstNodeName = namer::astNodeClassName;
//
//        Map<TypeSpec, InterfaceDescription> interfaceDescriptionMap = new HashMap<>();
//        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "E"),
//                                    new InterfaceDescription(getBnfRule(canonicalBNF, "E"), new ParserInfo.Full(getAstNodeName.apply("E"))));
//        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "Prod_Sum"),
//                                    new InterfaceDescription(getBnfRule(canonicalBNF, "Prod_Sum"), new ParserInfo.Full(getAstNodeName.apply("E"))));
//        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "O"),
//                                    new InterfaceDescription(getBnfRule(canonicalBNF, "O"), new ParserInfo.PartialS(getAstNodeName.apply("E"), Set.of(getAstNodeName.apply("N")))));
//        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "O1"),
//                                    new InterfaceDescription(getBnfRule(canonicalBNF, "O1"), new ParserInfo.PartialD(getAstNodeName.apply("Sum"), Set.of("n0"))));
//        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "O2"),
//                                    new InterfaceDescription(getBnfRule(canonicalBNF, "O2"), new ParserInfo.PartialD(getAstNodeName.apply("Prod"), Set.of("n0"))));
//        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "NegExpr"),
//                                    new InterfaceDescription(getBnfRule(canonicalBNF, "NegExpr"), new ParserInfo.Full(getAstNodeName.apply("NegExpr"))));
//        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "N"),
//                                    new InterfaceDescription(getBnfRule(canonicalBNF, "N"), new ParserInfo.Full(getAstNodeName.apply("N"))));
//        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "N1"),
//                                    new InterfaceDescription(getBnfRule(canonicalBNF, "N1"), new ParserInfo.Full(getAstNodeName.apply("N1"))));
//        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "N2"),
//                                    new InterfaceDescription(getBnfRule(canonicalBNF, "N2"), new ParserInfo.Full(getAstNodeName.apply("N2"))));
//        interfaceDescriptionMap.put(getInnerTypeRecursively(fluentApi.typeSpec, "M"),
//                                    new InterfaceDescription(getBnfRule(canonicalBNF, "M"), ParserInfo.BRIDGE));
//
//        final var fluentInterfaces = fluentApi.typeSpec.typeSpecs;
//        final var astNodeTypes = ast.typeSpec.typeSpecs.stream()
//                .collect(Collectors.toMap(ty -> namer.astNodeClassName(ty.name), Function.identity()));
//        JavaFile apiImpl = new ApiImplGenerator(namer, fluentInterfaces, interfaceToAstNodeMap, interfaceDescriptionMap, astNodeTypes)
//                .generateJavaFile();
//
//        String astCode = ast.toString();
//        String apiCode = fluentApi.toString();
//        String apiImplCode = apiImpl.toString();
//
//        System.out.println("// AST");
//        System.out.println();
//        System.out.println(astCode);
//        System.out.println();
//        System.out.println("// API");
//        System.out.println(apiCode);
//        System.out.println();
//        System.out.println("// API Implementation");
//        System.out.println(apiImplCode);
//
//        Path destPath = Path.of("src/test/generated-sources/").toAbsolutePath();
//
//        try {
//            Files.createDirectories(destPath);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        Stream.of(ast, fluentApi, apiImpl)
//                .forEach(jf -> {
//                    try {
//                        jf.writeTo(destPath);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                });
//    }
//
//    static Rule getBnfRule(BNF bnf, CharSequence varName) {
//        return bnf.rules().stream()
//                .filter(rule -> rule.lhs().name().toString().contentEquals(varName))
//                .findFirst()
//                .orElseThrow(() -> new IllegalArgumentException("No rule for variable %s".formatted(varName)));
//    }
//
//}
