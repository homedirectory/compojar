package compojar.bnf;

import java.lang.reflect.Type;
import java.util.Formattable;
import java.util.Formatter;

public record Parameter(Type type, CharSequence name) implements Formattable {
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
        formatter.format("%s %s", type.getTypeName(), name);
    }

}
