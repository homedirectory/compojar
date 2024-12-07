package compojar.stack;

import compojar.bnf.BNF;
import compojar.bnf.Derivation;
import compojar.bnf.Selection;
import compojar.bnf.Terminal;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static compojar.bnf.Derivation.derivation;
import static java.util.stream.Collectors.toSet;

public record StackMachine (Set<Symbol> inputSymbols, Set<Symbol> stackSymbols, Set<Rule> rules, Symbol start) {

    public StackMachine(Set<? extends Rule> rules, Symbol start) {
        this(getInputSymbols(rules), getStackSymbols(rules), Set.copyOf(rules), start);
    }

    public Set<Rule> rulesThatPop(Symbol symbol) {
        return rules.stream().filter(rule -> rule.pops().equals(symbol)).collect(toSet());
    }

    private static Set<Symbol> getInputSymbols(final Set<? extends Rule> rules) {
        return rules.stream().map(Rule::reads).collect(toSet());
    }

    private static Set<Symbol> getStackSymbols(final Set<? extends Rule> rules) {
        return rules.stream()
                .flatMap(rule -> Stream.concat(Stream.of(rule.pops()), rule.pushes().stream()))
                .collect(toSet());
    }

    public static StackMachine fromBNF(final BNF bnf) {
        var rules = bnf.rules().stream()
                .flatMap(rule -> switch (rule) {
                    case Derivation derivation -> Stream.of(derivation);
                    case Selection selection -> selection.rhs().stream().map(var -> derivation(selection.lhs(), var));
                })
                .map(rule -> {
                    final Symbol reads;
                    final List<Symbol> pushes;
                    if (!rule.rhs().isEmpty() && rule.rhs().getFirst() instanceof Terminal t) {
                        reads = Symbol.symbol(t);
                        pushes = rule.rhs().subList(1, rule.rhs().size())
                                .stream()
                                .map(Symbol::symbol)
                                .toList();
                    }
                    else {
                        reads = Symbol.empty;
                        pushes = rule.rhs().stream().map(Symbol::symbol).toList();
                    }

                    var pops = Symbol.symbol(rule.lhs());
                    return new Rule(reads, pops, pushes);
                })
                .collect(toSet());
        return new StackMachine(rules, Symbol.symbol(bnf.start()));
    }

    @Override
    public String toString() {
        return """
               start: %s
               rules:
               %s""".formatted(start, rules.stream().map(Rule::toString).collect(Collectors.joining("\n")));
    }

}
