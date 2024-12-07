package compojar.stack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record Rule(Symbol reads, Symbol pops, List<Symbol> pushes) {

    public static Rule rule(Symbol reads, Symbol pops, Symbol... pushes) {
        return new Rule(reads, pops, Arrays.stream(pushes).filter(s -> s != Symbol.empty).toList());
    }

    @Override
    public String toString() {
        return "read: %s; pop: %s; push: %s".formatted(reads, pops, pushes.stream().map(Symbol::name).collect(Collectors.joining(" ")));
    }

}
