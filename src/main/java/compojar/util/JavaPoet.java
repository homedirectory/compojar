package compojar.util;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static javax.lang.model.element.Modifier.*;

public class JavaPoet {

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // : Substitutions
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    // public record Subst ()

    // Example
    // interface A<K> extends B<K>
    // interface B<K> extends C<K>
    // interface C<K>
    // A<X> a;

    public static Map<String, TypeName> getSubsts(final TypeSpec typeSpec, final TypeName typeName) {
        return getSubsts(typeSpec, typeName, Map.of());
    }


    public static Map<String, TypeName> getSubsts(TypeSpec typeSpec, TypeName typeName, Map<String, TypeName> subs) {
        if (!asClassName(typeName).simpleName().equals(typeSpec.name)) {
            throw new IllegalArgumentException(format("TypeSpec and TypeName do not match. TypeName: %s, TypeSpec's name: %s",
                                                      typeName, typeSpec.name));
        }

        if (typeName instanceof ParameterizedTypeName paramTypeName) {
            if (typeSpec.typeVariables.size() != paramTypeName.typeArguments.size()) {
                throw new IllegalArgumentException(format("Wrong number of type arguments. Expected %s, given %s. TypeSpec: %s, TypeName: %s",
                                                          typeSpec.typeVariables.size(), paramTypeName.typeArguments.size(), typeSpec.name, typeName));
            }

            // No zip, then.
            var substs = new HashMap<String, TypeName>();
            for (int i = 0; i < typeSpec.typeVariables.size(); i++) {
                substs.put(typeSpec.typeVariables.get(i).name, substTypes(paramTypeName.typeArguments.get(i), subs));
            }
            return unmodifiableMap(substs);
        }
        else if (typeName instanceof ClassName $) {
            return Map.of();
        }
        else {
            throw new IllegalArgumentException("Invalid TypeName %s to use for TypeSpec %s".formatted(typeName, typeSpec.name));
        }
    }

    public static TypeName substTypes(TypeName typeName, Map<String, TypeName> bindings) {
        return switch (typeName) {
            case ParameterizedTypeName it -> substTypes(it, bindings);
            case TypeVariableName it -> substTypes(it, bindings);
            case null, default -> typeName;
        };
    }

    public static ParameterizedTypeName substTypes(ParameterizedTypeName typeName, Map<String, TypeName> bindings) {
        final TypeName[] typeArgs = typeName.typeArguments.stream()
                .map(tyArg -> substTypes(tyArg, bindings))
                .toArray(TypeName[]::new);
        return ParameterizedTypeName.get(typeName.rawType, typeArgs);
    }

    public static TypeName substTypes(TypeVariableName typeVarName, Map<String, TypeName> bindings) {
        return bindings.getOrDefault(typeVarName.name, typeVarName);
    }

    public static MethodSpec substTypes(final MethodSpec method, final Map<String, TypeName> subs) {
        return method.toBuilder()
                .returns(substTypes(method.returnType, subs))
                // TODO parameters
                .build();
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // : Everything else
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    public static TypeSpec.Builder interfaceBuilder(final ClassName name) {
        return TypeSpec.interfaceBuilder(name)
                .addModifiers(PUBLIC, STATIC);
    }

    public static ParameterSpec parameter(final FieldSpec fieldSpec, final Modifier... modifiers) {
        return ParameterSpec.builder(fieldSpec.type, fieldSpec.name, modifiers).build();
    }

    public static String simpleClassName(final TypeName typeName) {
        return asClassName(typeName).simpleName();
    }

    public static ClassName asClassName(final TypeName typeName) {
        return switch (typeName) {
            case ClassName it -> it;
            case ParameterizedTypeName it -> it.rawType;
            default -> throw new IllegalStateException("Unexpected value: " + typeName);
        };
    }

    public static Stream<TypeName> allSuperInterfaces(TypeSpec typeSpec, Function<ClassName, TypeSpec> typeFinder) {
        var uniqueNames = new HashSet<ClassName>();
        return allSuperInterfaces_(typeSpec, typeFinder, uniqueNames);
    }

    private static Stream<TypeName> allSuperInterfaces_(TypeSpec typeSpec, Function<ClassName, TypeSpec> typeFinder,
                                                        Set<ClassName> uniqueNames)
    {
        return typeSpec.superinterfaces.stream()
                .filter(tyName -> {
                    var className = asClassName(tyName);
                    final var seen = uniqueNames.contains(className);
                    if (!seen)
                        uniqueNames.add(className);
                    return !seen;
                })
                .flatMap(tyName -> Stream.concat(Stream.of(tyName),
                                                 allSuperInterfaces_(typeFinder.apply(asClassName(tyName)), typeFinder, uniqueNames)));
    }

    public static TypeSpec assertInterface(final TypeSpec typeSpec) {
        if (typeSpec.kind != TypeSpec.Kind.INTERFACE) {
            throw new AssertionError("Not an interface: " + typeSpec);
        }
        return typeSpec;
    }

    public static TypeSpec.Builder recordBuilder(ClassName name, Iterable<? extends FieldSpec> components) {
        var builder = classBuilder(name);
        components.forEach(field -> builder.addField(
                FieldSpec.builder(field.type, field.name, PUBLIC, FINAL)
                        .build()));

        final var constructorBuilder = MethodSpec.constructorBuilder();
        constructorBuilder.addModifiers(PUBLIC);
        components.forEach(field -> constructorBuilder.addParameter(field.type, field.name, FINAL));
        components.forEach(field -> constructorBuilder.addStatement("this.%s = %s".formatted(field.name, field.name)));
        builder.addMethod(constructorBuilder.build());

        return builder;
    }

    public static MethodSpec.Builder methodBuilder(MethodSpec method) {
        return MethodSpec.methodBuilder(method.name)
                .returns(method.returnType)
                .addParameters(method.parameters);
    }

    public static MethodSpec recordStyleConstructor(Iterable<? extends FieldSpec> fields, Optional<CodeBlock> beforeFieldsStatement) {
        var builder = MethodSpec.constructorBuilder();
        beforeFieldsStatement.ifPresent(builder::addStatement);
        fields.forEach(field -> builder.addParameter(parameter(field, FINAL)));
        fields.forEach(field -> builder.addStatement("this.%s = %s".formatted(field.name, field.name)));
        return builder.build();
    }

    public static TypeSpec getInnerType(TypeSpec typeSpec, String name) {
        return typeSpec.typeSpecs.stream()
                .filter(ty -> ty.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such inner type %s in %s".formatted(name, typeSpec.name)));
    }

    public static TypeSpec getInnerTypeRecursively(TypeSpec typeSpec, String name) {
        return allInnerTypes(typeSpec)
                .filter(ty -> ty.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such inner type %s in %s".formatted(name, typeSpec.name)));
    }

    public static Stream<TypeSpec> allInnerTypes(TypeSpec typeSpec) {
        return typeSpec.typeSpecs.stream()
                .flatMap(ty -> Stream.concat(Stream.of(ty), allInnerTypes(ty)));
    }

    public static FieldSpec getField(TypeSpec typeSpec, CharSequence name) {
        return typeSpec.fieldSpecs.stream()
                .filter(field -> field.name.contentEquals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such field '%s' in type '%s'".formatted(name, typeSpec.name)));
    }

}
