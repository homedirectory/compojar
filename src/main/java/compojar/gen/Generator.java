package compojar.gen;

import com.squareup.javapoet.JavaFile;
import compojar.bnf.BNF;
import compojar.bnf.EmptyProductionElimination;
import compojar.bnf.LeftFactoring;
import compojar.bnf.TerminalNormalisation;
import compojar.stack.StackMachine;
import compojar.util.T2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static compojar.util.JavaPoet.getInnerTypeRecursively;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class Generator {

    private final Namer namer;
    private final BNF bnf;

    public Generator(final Namer namer, final BNF bnf) {
        this.namer = namer;
        this.bnf = validateBnf(bnf);
    }

    private static BNF validateBnf(BNF bnf) {
        assertNoEmptyRhs(bnf);
        return bnf;
    }

    private static void assertNoEmptyRhs(BNF bnf) {
        assertNoEmptyRhs(bnf, () -> "");
    }

    private static void assertNoEmptyRhs(BNF bnf, Supplier<String> errMsgFn) {
        var emptyRules = bnf.rules().stream()
                .filter(r -> r.rhs().isEmpty())
                .toList();
        if (!emptyRules.isEmpty()) {
            var auxErrMsg = errMsgFn.get();
            var mainErrMsg = format("Rules with empty RHS are disallowed. Illegal rules:\n%s",
                                    emptyRules.stream().map(Objects::toString).collect(joining("\n")));
            throw new IllegalArgumentException(auxErrMsg.isBlank() ? mainErrMsg : String.join("\n", auxErrMsg, mainErrMsg));
        }
    }

    public void generate(Path outputDirectory) {
        new AstGenerator(namer, bnf).generate()
                .run((astJavaFile, _astMetadata) -> {
                    var _canonicalBnf = bnf;
                    {
                        var termNormResult = new TerminalNormalisation(namer).apply(_canonicalBnf, _astMetadata);
                        _canonicalBnf = termNormResult.fst();
                        _astMetadata = termNormResult.snd();
                    }

                    {
                        var result = new EmptyProductionElimination(namer).apply(_canonicalBnf, _astMetadata);
                        _canonicalBnf = result.map(T2::fst).orElse(_canonicalBnf);
                        _astMetadata = result.map(T2::snd).orElse(_astMetadata);
                    }

                    {
                        var leftFactorResult = new LeftFactoring(namer).apply(new LeftFactoring.Data(_canonicalBnf, _astMetadata));
                        _canonicalBnf = leftFactorResult.bnf();
                        _astMetadata = leftFactorResult.astMetadata();
                    }

                    {
                        var result = new EmptyProductionElimination(namer).apply(_canonicalBnf, _astMetadata);
                        _canonicalBnf = result.map(T2::fst).orElse(_canonicalBnf);
                        _astMetadata = result.map(T2::snd).orElse(_astMetadata);
                    }

                    final var canonicalBNF = _canonicalBnf;
                    final var astMetadata = _astMetadata;

                    assertNoEmptyRhs(canonicalBNF, () -> "Grammar contains ambiguous rules (even after rewriting).");

                    var stackMachine = StackMachine.fromBNF(canonicalBNF);
                    new ApiGenerator(namer, stackMachine).generate().run((apiJavaFile, symbolInterfaceMap) -> {
                        var interfaceAstNodeMap = symbolInterfaceMap.entrySet().stream()
                                .map(stackSym_inter -> {
                                    return astMetadata.getParserInfo(canonicalBNF.getVariable(stackSym_inter.getKey()))
                                            .flatMap(ParserInfo::maybeAstNodeName)
                                            .map(astNodeName -> T2.t2(stackSym_inter.getValue(),
                                                                      getInnerTypeRecursively(astJavaFile.typeSpec, astNodeName.simpleName())));
                                })
                                .flatMap(Optional::stream)
                                .collect(toMap(T2::fst, T2::snd));
                        var interfaceDescriptionMap = symbolInterfaceMap.keySet().stream()
                                .filter(stackSym -> stackSym != ApiGenerator.END_SYMBOL)
                                .collect(toMap(symbolInterfaceMap::get,
                                                          stackSym -> new InterfaceDescription(canonicalBNF.requireRuleFor(canonicalBNF.getVariable(stackSym)),
                                                                                               astMetadata.requireParserInfo(canonicalBNF.getVariable(stackSym)))));
                        final var fluentInterfaces = apiJavaFile.typeSpec.typeSpecs;
                        final var astNodeTypes = astJavaFile.typeSpec.typeSpecs.stream()
                                .collect(toMap(ty -> namer.astNodeClassName(ty.name), Function.identity()));
                        JavaFile apiImplJavaFile = new ApiImplGenerator(namer, fluentInterfaces, interfaceAstNodeMap, interfaceDescriptionMap, astNodeTypes)
                                .generateJavaFile();

                        String astCode = astJavaFile.toString();
                        String apiCode = apiJavaFile.toString();
                        String apiImplCode = apiImplJavaFile.toString();

                        System.out.println("// AST");
                        System.out.println();
                        System.out.println(astCode);
                        System.out.println();
                        System.out.println("// API");
                        System.out.println(apiCode);
                        System.out.println();
                        System.out.println("// API Implementation");
                        System.out.println(apiImplCode);

                        Path destPath = outputDirectory.toAbsolutePath();

                        try {
                            Files.createDirectories(destPath);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        Stream.of(astJavaFile, apiJavaFile, apiImplJavaFile)
                                .forEach(jf -> {
                                    try {
                                        jf.writeTo(destPath);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                    });
                });
    }

}
