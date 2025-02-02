package compojar.bnf;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import static compojar.util.Util.append;
import static compojar.util.Util.removeAll;
import static java.lang.String.format;
import static java.util.stream.Collectors.*;

final class TerminalRecord implements Terminal {

    private final CharSequence name;
    private final List<Parameter> parameters;

    TerminalRecord(CharSequence name, List<Parameter> parameters) {
        var dupNames = parameters.stream()
                .collect(collectingAndThen(groupingBy(Parameter::name),
                                           map -> removeAll(map, ($, ps) -> ps.size() < 2).keySet()));
        if (!dupNames.isEmpty()) {
            throw new IllegalArgumentException(format("Parameters with duplicate names are disallowed: %s(%s)",
                                                      name,
                                                      parameters.stream().map("%s"::formatted).collect(joining(", "))));
        }

        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public Terminal parameters(Type type, CharSequence paramName) {
        return new TerminalRecord(name, append(parameters, new Parameter(type, paramName)));
    }

    @Override
    public CharSequence name() {
        return name;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || obj instanceof Terminal that
                && name.equals(that.name())
                && parameters.equals(that.getParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameters);
    }

    @Override
    public String toString() {
        return format("%s%s",
                             name,
                             parameters.isEmpty()
                                     ? ""
                                     : parameters.stream()
                                             .map(p -> "%s %s".formatted(p.type().getTypeName(), p.name()))
                                             .collect(joining(", ", "(", ")")));
    }

}
