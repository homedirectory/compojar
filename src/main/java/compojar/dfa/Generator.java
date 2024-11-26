package compojar.dfa;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.Set;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static java.util.stream.Collectors.toSet;

public class Generator {

    private static final TypeSpec END_TYPE = interfaceBuilder("$End")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addMethod(methodBuilder("$")
                               .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                               .returns(void.class)
                               .build())
            .build();

    public void generateApi(String name, DFA dfa) {
        final String pkgName = "";
        final TypeName endTypeName = ClassName.get(pkgName, END_TYPE.name);

        final Set<TypeSpec> stateTypes = dfa.states().stream()
                .map(state -> {
                    final var builder = interfaceBuilder(typeNameFromState(state))
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
                    if (dfa.isAccepting(state)) {
                        builder.addSuperinterface(endTypeName);
                    }
                    dfa.transitionsFrom(state)
                            .map(rule -> methodBuilder(rule.symbol())
                                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                                    .returns(ClassName.get(pkgName, typeNameFromState(rule.destination())))
                                    .build())
                            .forEach(builder::addMethod);
                    return builder.build();
                })
                .collect(toSet());

        final var topLevelInterface = interfaceBuilder(name)
                .addMethod(methodBuilder(name)
                                   .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                   .returns(ClassName.get(pkgName, typeNameFromState(dfa.start())))
                                   .addStatement("throw new $T()", UnsupportedOperationException.class)
                                   .build())
                .addType(END_TYPE)
                .addTypes(stateTypes)
                .build();

        System.out.println(JavaFile.builder(pkgName, topLevelInterface).build().toString());
    }

    private String typeNameFromState(String state) {
        return state;
    }

}
