package compojar.dfa;

import java.util.Set;
import java.util.stream.Stream;

record DFA
        (Set<String> states, Set<String> symbols, Set<Rule> rules, Set<String> acceptingStates, String start)
{

    public DFA {
        if (!states.containsAll(acceptingStates)) {
            throw new IllegalArgumentException("""
            Accepting states must be a subset of all states.
            DFA: %s""".formatted(this));
        }

        if (!states.contains(start)) {
            throw new IllegalArgumentException("""
            Start state not a member of all states.
            DFA: %s""".formatted(this));
        }
    }

    public boolean isAccepting(String state) {
        return acceptingStates.contains(state);
    }

    public Stream<Rule> transitionsFrom(final String state) {
        return rules.stream()
                .filter(rule -> rule.source().equals(state));

    }

}
