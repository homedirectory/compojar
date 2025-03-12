package compojar.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public final class Reflection {

    public static Stream<Field> streamAllFields(Class<?> type) {
        return streamHierarchy(type)
                .flatMap(ty -> Arrays.stream(ty.getDeclaredFields()));
    }

    public static Stream<Class> streamHierarchy(Class<?> type) {
        return Stream.iterate(type, Objects::nonNull, Class::getSuperclass);
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private Reflection() {}

}
