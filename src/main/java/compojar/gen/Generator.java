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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static compojar.util.JavaPoet.getInnerTypeRecursively;
import static java.util.stream.Collectors.toMap;

public class Generator {

    private final Namer namer;
    private final BNF bnf;

    public Generator(final Namer namer, final BNF bnf) {
        this.namer = namer;
        this.bnf = bnf;
    }

    public void generate(Path outputDirectory) throws IOException {
        new AstGenerator(namer, bnf).generate()
                .run((astJavaFile, _astMetadata) -> {
                    var _canonicalBnf = bnf;
                    {
                        var termNormResult = new TerminalNormalisation(namer).apply(_canonicalBnf, _astMetadata);
                        _canonicalBnf = termNormResult.fst();
                        _astMetadata = termNormResult.snd();
                    }

                    _canonicalBnf = new EmptyProductionElimination(namer).apply(_canonicalBnf);

                    {
                        var leftFactorResult = new LeftFactoring(namer).apply(new LeftFactoring.Data(_canonicalBnf, _astMetadata));
                        _canonicalBnf = leftFactorResult.bnf();
                        _astMetadata = leftFactorResult.astMetadata();
                    }

                    _canonicalBnf = new EmptyProductionElimination(namer).apply(_canonicalBnf);

                    final var canonicalBNF = _canonicalBnf;
                    final var astMetadata = _astMetadata;

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
                        // .forEach((stackSym, inter) -> interfaceDescriptionMap.put(inter, new InterfaceDescription(canonicalBNF.requireRuleFor(canonicalBNF.getVariable(stackSym)),
                        //                                                                                                     astMetadata.requireParserInfo(canonicalBNF.getVariable(stackSym)))));
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

                        Path destPath = Path.of("src/test/generated-sources/").toAbsolutePath();

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
