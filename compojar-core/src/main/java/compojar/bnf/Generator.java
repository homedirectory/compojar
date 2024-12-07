// package compojar.bnf;
//
// import com.squareup.javapoet.*;
//
// import java.io.IOException;
// import java.util.List;
// import java.util.Optional;
// import java.util.function.Predicate;
//
// import static com.squareup.javapoet.MethodSpec.methodBuilder;
// import static compojar.util.Util.append;
// import static compojar.util.Util.enumeratedStream;
// import static java.util.stream.Collectors.toSet;
// import static javax.lang.model.element.Modifier.*;
//
// /**
//  * Generates a fluent API from a BNF in a specific form.
//  */
// public class Generator {
//
//     private final TypeVariableName k = TypeVariableName.get("K");
//
//     public String generateApi(final BNF bnf, final String name) {
//         var sb = new StringBuilder();
//         generateApi(bnf, name, sb);
//         return sb.toString();
//     }
//
//     public void generateApi(final BNF bnf, final String name, final Appendable appendable) {
//         final String pkgName = "";
//
//         final var endTypeName = ClassName.get(pkgName, "$End$");
//         final TypeSpec endType = interfaceBuilder(endTypeName)
//                 .addTypeVariable(k)
//                 .addMethod(methodBuilder("$")
//                                    .returns(k)
//                                    .addModifiers(PUBLIC, ABSTRACT)
//                                    .build())
//                 .build();
//
//         final TypeSpec topInterface = interfaceBuilder(name)
//                 .addTypes(generateInterfaces(bnf, endTypeName))
//                 .addType(endType)
//                 .addMethod(methodBuilder("start")
//                                    .returns(parameterisedFold(List.of(interfaceName(bnf.start()), ClassName.OBJECT)))
//                                    .addModifiers(PUBLIC, STATIC)
//                                    .addStatement("return null")
//                                    .build())
//                 .build();
//
//         final JavaFile javaFile = JavaFile.builder(pkgName, topInterface).build();
//         try {
//             javaFile.writeTo(appendable);
//         } catch (IOException e) {
//             throw new RuntimeException(e);
//         }
//     }
//
//     /*
//        1. Transform the BNF so that each rule has one of the following forms:
//           a. <A> ::= b C*
//           b. <A> ::= C*
//           I.e., the right-hand side can contain at most one terminal, which must be the first symbol.
//        2. For each non-terminal, create an interface parameterised with a single type variable K representing the continuation.
//        3. For each rule of the form <A> ::= b C*, in the interface corresponding to <A> define a method named 'b',
//           whose return type is the result of folding C* + K (sequence C1, ..., Cn, K) using function parameterise.
//           I.e., the return type is (parameterise(C1, parameterise(C2, ...))). If C* is empty, the return type is simply K.
//        4. For each rule of the form <A> ::= C1 C*, make the interface corresponding to <A> extend parameterise(C1, ..., Cn, K).
//
//        parameterise(T, U) produces type T parameterised with type U.
//        For example,
//        * parameterise(List, String) = List<String>
//        * parameterise(List, Set<String>) = List<Set<String>>
//
//        TODO empty RHS is supported only for the starting state
//      */
//     private List<TypeSpec> generateInterfaces(final BNF bnf, final ClassName endTypeName) {
//         final var tBnf = transformBnf(bnf);
//         return tBnf.variables().stream()
//                 .map(v -> Optional.of(bnf.rulesFor(v).toList()).filter(Predicate.not(List::isEmpty)).map(rules -> buildInterface(v, rules, tBnf, endTypeName)))
//                 .flatMap(Optional::stream)
//                 .toList();
//     }
//
//     private TypeSpec buildInterface(final Variable v, final List<Rule> rules, final BNF bnf, final ClassName endTypeName) {
//         if (rules.isEmpty()) throw new IllegalArgumentException(String.format("empty rules for %s", v));
//
//         final var builder = interfaceBuilder(interfaceName(v)).addTypeVariable(k);
//
//         for (final Rule rule : rules) {
//             if (!rule.rhs().isEmpty()) {
//                 switch (rule.rhs().getFirst()) {
//                     case Terminal terminal ->
//                             builder.addMethod(methodBuilder(terminal.name().toString())
//                                                       .returns(parameterisedFold(append(rule.rhs().subList(1, rule.rhs().size()).stream().map(sym -> interfaceName((Variable) sym)).toList(),
//                                                                                         k)))
//                                                       .addModifiers(PUBLIC, ABSTRACT)
//                                                       .build());
//                     case Variable $ -> builder.addSuperinterface(parameterisedFold(append(rule.rhs().stream().map(sym -> interfaceName((Variable) sym)).toList(),
//                                                                                           k)));
//                 }
//             }
//             else {
//                 builder.addSuperinterface(ParameterizedTypeName.get(endTypeName, k));
//             }
//         }
//
//         return builder.build();
//     }
//
//     private TypeName parameterisedFold(final List<TypeName> types) {
//         if (types.isEmpty()) throw new IllegalArgumentException("empty list");
//
//         return types.size() == 1
//                 ? types.getFirst()
//                 : ParameterizedTypeName.get((ClassName) types.getFirst(), parameterisedFold(types.subList(1, types.size())));
//     }
//
//     private BNF transformBnf(final BNF bnf) {
//         final var newRules = bnf.rules().stream()
//                 .map(rule -> transformRule(rule))
//                 .collect(toSet());
//         return new BNF(newRules, bnf.start());
//     }
//
//     private Rule transformRule(final Rule rule) {
//         return rule.rhs().isEmpty()
//                 ? rule
//                 : new Rule(rule.lhs(),
//                            enumeratedStream(rule.rhs().stream(),
//                                             (sym, i) -> i > 0 && sym instanceof Terminal t ? genVar(t) : sym)
//                                    .toList());
//     }
//
//     private ClassName interfaceName(final Variable v) {
//         return ClassName.get("", v.name().toString());
//     }
//
//     private Variable genVar(final CharSequence name) {
//         final var genName = "G" + genVarCount + "_" + name;
//         genVarCount++;
//         return new Variable() {
//             @Override
//             public CharSequence name() {
//                 return genName;
//             }
//
//             @Override
//             public boolean equals(final Object obj) {
//                 return this == obj;
//             }
//         };
//     }
//     private int genVarCount = 0;
//
//     private static TypeSpec.Builder interfaceBuilder(final String name) {
//         return TypeSpec.interfaceBuilder(name)
//                 .addModifiers(PUBLIC, STATIC);
//     }
//
//     private static TypeSpec.Builder interfaceBuilder(final ClassName name) {
//         return TypeSpec.interfaceBuilder(name)
//                 .addModifiers(PUBLIC, STATIC);
//     }
//
// }
