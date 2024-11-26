package compojar.gen;

import com.squareup.javapoet.ClassName;

import java.util.Optional;
import java.util.Set;

public sealed interface ParserInfo {

    Optional<ClassName> maybeAstNodeName();

    record Full (ClassName astNode) implements ParserInfo {
        public PartialD toPartialD(Set<String> components) {
            return new PartialD(astNode, components);
        }

        public PartialS toPartialS(Set<ClassName> components) {
            return new PartialS(astNode, components);
        }

        @Override
        public Optional<ClassName> maybeAstNodeName() {
            return Optional.of(astNode);
        }
    }

    /**
     * Partial parser of a selection rule.
     *
     * @param astNode
     * @param components  type names of components that need to be supplied externally
     */
    record PartialS (ClassName astNode, Set<ClassName> components) implements ParserInfo {
        @Override
        public Optional<ClassName> maybeAstNodeName() {
            return Optional.of(astNode);
        }
    }

    /**
     * Partial parser of a derivation rule.
     *
     * @param astNode
     * @param components  names of components that need to be supplied externally
     */
    record PartialD (ClassName astNode, Set<String> components) implements ParserInfo {
        @Override
        public Optional<ClassName> maybeAstNodeName() {
            return Optional.of(astNode);
        }
    }

    /**
     * Partial parsers of a derivation rule with an empty RHS.
     */
    record PartialEmpty (ClassName astNode) implements ParserInfo {
        @Override
        public Optional<ClassName> maybeAstNodeName() {
            return Optional.of(astNode);
        }
    }

    Bridge BRIDGE = new Bridge();

    final class Bridge implements ParserInfo {
        private Bridge() {}

        @Override
        public Optional<ClassName> maybeAstNodeName() {
            return Optional.empty();
        }

    }
}
