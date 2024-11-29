package compojar.bnf;

import compojar.util.T2;
import compojar.util.Util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static compojar.util.Util.contentEquals;
import static compojar.util.Util.removeAll;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;

// TODO enhance with metadata:
// * names for fields of AST nodes
public record BNF (Set<Terminal> terminals, Set<Variable> variables, Set<Rule> rules, Variable start) {

    public BNF {
        validateRules(rules, start);
    }

    private static void validateRules(final Set<Rule> rules, final Variable start) {
        if (rules.stream().noneMatch(rule -> start.equals(rule.lhs()))) {
            throw new IllegalArgumentException("Missing a rule for the start variable %s".formatted(start));
        }

        final var illegalRules = rules.stream()
                .collect(collectingAndThen(groupingBy(Rule::lhs),
                                           map -> removeAll(map, (lhs, rs) -> rs.size() > 1)
                                                   .values().stream()
                                                   .flatMap(Collection::stream)
                                                   .sorted(comparing(Rule::lhs))
                                                   .toList()));
        if (!illegalRules.isEmpty()) {
            throw new IllegalArgumentException(
                    format("BNF cannot contain multiple rules for the same LHS. Invalid rules:\n%s",
                           illegalRules.stream().map(Objects::toString).collect(joining("\n"))));
        }
    }

    public BNF(Collection<Rule> rules, Variable start) {
        this(allTerminals(rules), allVariables(rules), new LinkedHashSet<>(rules), start);
    }

    public Variable getVariable(CharSequence name) {
        return variables.stream()
                .filter(var -> contentEquals(var.name(), name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such BNF variable: %s".formatted(name)));
    }

    public Optional<Rule> ruleFor(final Variable v) {
        return rules.stream()
                .filter(rule -> rule.lhs().equals(v))
                .findFirst();
                // .orElseThrow(() -> new IllegalArgumentException("No such rule: %s".formatted(v)));
    }

    public Rule requireRuleFor(final Variable v) {
        return rules.stream()
                .filter(rule -> rule.lhs().equals(v))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such rule: %s".formatted(v)));
    }

    public Selection requireSelectionFor(final Variable v) {
        return rules.stream()
                .filter(rule -> rule.lhs().equals(v))
                .findFirst()
                .map(rule -> {
                    if (rule instanceof Selection it) {
                        return it;
                    }
                    throw new IllegalStateException("Expected rule %s to be a selection, but was %s".formatted(v, rule.getClass()));
                })
                .orElseThrow(() -> new IllegalArgumentException("No such rule: %s".formatted(v)));
    }

    public Stream<Rule> rulesFor(final Variable v) {
        return rules.stream()
                .filter(rule -> rule.lhs().equals(v));
        // .orElseThrow(() -> new IllegalArgumentException("No such rule: %s".formatted(v)));
    }

    public Stream<Derivation> rulesForOptions(final Selection selection) {
        return selection.rhs().stream()
                .map(var -> ruleFor(var).orElseThrow(() -> new IllegalStateException("No derivation for variable %s".formatted(var))))
                .map(rule -> (Derivation) rule);
    }

    @Override
    public String toString() {
        return rules.stream()
                .sorted(comparing(r -> r.lhs().name().toString()))
                .map(Objects::toString)
                .collect(joining("\n"));
    }

    private static Set<Terminal> allTerminals(Collection<Rule> rules) {
        return rules.stream()
                .flatMap(Rule::allSymbols)
                .filter(sym -> sym instanceof Terminal)
                .map(sym -> (Terminal) sym)
                .collect(toSet());
    }

    private static Set<Variable> allVariables(Collection<Rule> rules) {
        return rules.stream()
                .flatMap(Rule::allSymbols)
                .filter(sym -> sym instanceof Variable)
                .map(sym -> (Variable) sym)
                .collect(toSet());
    }

    public BNF addRules(final Collection<? extends Rule> rules) {
        return rules.isEmpty()
                ? this
                : new BNF(Util.concatList(this.rules, rules), start);
    }

    // public BNF removeVariables(final Collection<? extends Variable> variables) {
    //     if (variables.stream().anyMatch(v -> v.equals(start))) {
    //         throw new IllegalArgumentException("Start variable %s cannot be removed".formatted(start));
    //     }
    //     return variables.isEmpty()
    //             ? this
    //             : new BNF(this.rules.stream().filter(r -> !variables.contains(r)).collect(toSet()), start);
    // }

    public BNF overrideRule(Rule rule) {
        return updateRule(rule.lhs(), $ -> rule);
    }

    public BNF updateRule(Variable lhs, Function<? super Rule, ? extends Rule> fn) {
        var rule = ruleFor(lhs).orElseThrow(() -> new IllegalArgumentException("No such rule: %s".formatted(lhs)));
        var newRule = fn.apply(rule);
        return new BNF(rules.stream()
                               .map(r -> r.lhs().equals(lhs) ? newRule : r)
                               .toList(),
                       start);
    }

    public BNF updateRules(final Function<? super Rule, ? extends Rule> fn) {
        return new BNF(rules.stream().map(fn).collect(toSet()), start);
    }

    private List<Variable> findUnused() {
        return variables.stream()
                .filter(var -> !var.equals(start))
                .filter(var -> rules.stream()
                        .filter(r -> !r.lhs().equals(var))
                        .noneMatch(r -> r.rhs().contains(var)))
                .toList();
    }

    public T2<BNF, List<Variable>> removeUnused() {
        var unusedVars = findUnused();
        if (unusedVars.isEmpty()) {
            return T2.t2(this, List.of());
        }
        else {
            var newRules = rules.stream()
                    .filter(r -> !unusedVars.contains(r.lhs()))
                    .toList();
            final var result = new BNF(newRules, start).removeUnused();
            return result.map2(restUnused -> Util.concatList(unusedVars, restUnused));
        }
    }

    public boolean semanticEquals(final BNF bnf) {
        if (!start.equals(bnf.start)) {
            return false;
        }

        // Not fully accurate if there are unused rules.
        var rulesIter = rules.stream().sorted(comparing(Rule::lhs)).iterator();
        var theirRulesIter = bnf.rules.stream().sorted(comparing(Rule::lhs)).iterator();
        while (rulesIter.hasNext() && theirRulesIter.hasNext()) {
           if (!rulesIter.next().semanticEquals(theirRulesIter.next()))
               return false;
        }
        return true;
    }

}
