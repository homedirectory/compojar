package compojar.bnf;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparing;

public non-sealed interface Terminal extends Symbol {

    Comparator<Terminal> comparator = comparing(t -> t.name().toString());

    default List<Parameter> getParameters() {
        return List.of();
    }

    default Terminal parameters(Type type, CharSequence name) {
        return new TerminalRecord(name(), List.of(new Parameter(type, name)));
    }

    default Terminal parameters(Type type1, CharSequence name1, Type type2, CharSequence name2) {
        return parameters(type1, name1).parameters(type2, name2);
    }

}
