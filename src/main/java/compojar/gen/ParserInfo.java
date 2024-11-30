package compojar.gen;

import com.squareup.javapoet.ClassName;
import compojar.bnf.Variable;

import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

public sealed interface ParserInfo {

    Optional<ClassName> maybeAstNodeName();

    Optional<Variable> implicitVar();

    default ClassName requireAstNodeName() {
        return maybeAstNodeName().orElseThrow(() -> new IllegalStateException(
                format("This parser info doesn't have an associated AST node! Parser info: %s", this)));
    }

    /**
     * @param implicitVar  optional, name of an AST node implicitly parsed by this parser.
     *                         <p> Is present only if the first variable in the RHS of this rule derives an empty string.
     *                         Implicit parsing of abstract AST nodes is not supported (i.e., when the first variable in the RHS is a selection).
     */
    record Full (ClassName astNode, Optional<Variable> implicitVar) implements ParserInfo {

        public Full(ClassName astNode) {
            this(astNode, Optional.empty());
        }

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

        public Full setImplicitVar(Variable value) {
            implicitVar.ifPresent(it -> {
                throw new IllegalStateException(format("Implicit variable is already present in this parser: %s", it));
            });
            return new Full(astNode, Optional.of(value));
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

        @Override
        public Optional<Variable> implicitVar() {
            return Optional.empty();
        }
    }

    /**
     * Partial parser of a derivation rule.
     *
     * @param astNode
     * @param components  names of components that need to be supplied externally
     * @param implicitVar  optional, name of an AST node implicitly parsed by this parser.
     *                         <p> Is present only if the first variable in the RHS of this rule derives an empty string.
     *                         Implicit parsing of abstract AST nodes is not supported (i.e., when the first variable in the RHS is a selection).
     */
    // TODO we should also know about the partial parser that should be used for the implicit ast node
    record PartialD (ClassName astNode, Set<String> components, Optional<Variable> implicitVar) implements ParserInfo {

        public PartialD(ClassName astNode, Set<String> components) {
            this(astNode, components, Optional.empty());
        }

        @Override
        public Optional<ClassName> maybeAstNodeName() {
            return Optional.of(astNode);
        }

        public PartialD setImplicitVar(Variable value) {
            implicitVar.ifPresent(it -> {
                throw new IllegalStateException(format("Implicit AST node is already present in this parser: %s", it));
            });
            return new PartialD(astNode, components, Optional.of(value));
        }
    }

    Bridge BRIDGE = new Bridge();

    final class Bridge implements ParserInfo {
        private Bridge() {}

        @Override
        public Optional<ClassName> maybeAstNodeName() {
            return Optional.empty();
        }

        @Override
        public Optional<Variable> implicitVar() {
            return Optional.empty();
        }
    }
}
