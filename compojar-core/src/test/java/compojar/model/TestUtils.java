package compojar.model;

import compojar.util.T2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static compojar.model.Keys.CHILDREN;
import static compojar.model.Keys.NEXT;
import static compojar.util.Util.permutations;
import static compojar.util.Util.zipWith;

public final class TestUtils {

    /** BNF metadata key to identify symbols. */
    public static final compojar.bnf.Key<Object> id = new compojar.bnf.Key<>() {};

    /**
     * Structural equality of grammars.
     */
    public static Eq<GrammarTreeModel> grammarEq(Eq<GrammarNode> eqNode) {
        return (model1, model2) -> structEquals(model1, model2, eqNode);
    }

    public static boolean structEquals(GrammarTreeModel model1, GrammarTreeModel model2, Eq<GrammarNode> eqNode) {
        return semanticEquals_(model1, model1.root(), model2, model2.root(), eqNode);
    }

    private static boolean semanticEquals_(GrammarTreeModel model1, GrammarNode node1, GrammarTreeModel model2, GrammarNode node2, Eq<GrammarNode> eqNode) {
        return eqNode.areEqual(node1, node2)
               && nextEquals(model1, node1, model2, node2, eqNode)
               // Check for emptiness, anyMatch returns false for an empty stream.
               && (model1.get(node1, CHILDREN).orElseGet(Set::of).isEmpty() && model2.get(node2, CHILDREN).orElseGet(Set::of).isEmpty()
                   || nodePermutations(model1.get(node1, CHILDREN).orElseGet(Set::of),
                                       model2.get(node2, CHILDREN).orElseGet(Set::of))
                           .anyMatch(pair -> pair.map((seq1, seq2) -> zipWith(seq1, seq2, (n1, n2) -> semanticEquals_(model1, n1, model2, n2, eqNode)).allMatch(b -> b))));
    }

    private static boolean nextEquals(
            GrammarTreeModel model1,
            GrammarNode node1,
            GrammarTreeModel model2,
            GrammarNode node2,
            Eq<GrammarNode> eqNode)
    {
        final var maybeNext1 = model1.get(node1, NEXT);
        final var maybeNext2 = model2.get(node2, NEXT);

        return maybeNext1.map(next1 -> maybeNext2.filter(next2 -> semanticEquals_(model1, next1, model2, next2, eqNode))
                                                 .isPresent())
                         .orElseGet(maybeNext2::isEmpty);
    }

    private static Stream<T2<List<GrammarNode>, List<GrammarNode>>> nodePermutations(
            Collection<GrammarNode> nodes1,
            Collection<GrammarNode> nodes2)
    {
        var nodes2List = new ArrayList<>(nodes2);
        return permutations(new ArrayList<>(nodes1)).map(seq1 -> T2.t2(seq1, nodes2List));
    }

    private TestUtils() {}

}
