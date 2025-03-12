package compojar.model;

import compojar.model.GrammarNode.Free;
import compojar.util.T2;
import compojar.util.T3;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

import static compojar.model.Inline.inline;
import static compojar.model.Keys.*;
import static compojar.util.T2.t2;
import static compojar.util.T3.t3;
import static compojar.util.Util.*;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

/**
 * Common prefix elimination for grammar trees.
 */
public final class CommonPrefix {

    public static GrammarTreeModel removeCommonPrefix(
            GrammarTreeModel model,
            Function<? super GrammarNode, ?> nodeClassifier,
            NodeFactory nodeFactory)
    {
        var commonPrefix = findCommonPrefix(model, model.root(), nodeClassifier);
        if (commonPrefix.isEmpty()) {
            return model;
        }
        else {
            var commonAncestor = commonAncestor(model, commonPrefix)
                    .orElseThrow(() -> new IllegalStateException(format("Common prefix has no common ancestor: [%s]", commonPrefix)));

            if (!(commonAncestor instanceof Free)) {
                throw new IllegalStateException(
                        format("Common ancestor of a common prefix must be a free node.\nCommon prefix: %s\nAncestor: %s",
                               commonPrefix, commonAncestor));
            }

            return _removeCommonPrefix(inline(model, commonAncestor, nodeFactory), nodeClassifier, nodeFactory, commonPrefix);
        }
    }

    public static GrammarTreeModel _removeCommonPrefix(
            GrammarTreeModel model,
            Function<? super GrammarNode, ?> nodeClassifier,
            NodeFactory nodeFactory,
            Set<GrammarNode> commonPrefix)
    {
        var commonAncestor = commonAncestor(model, commonPrefix)
                .orElseThrow(() -> new IllegalStateException(format("Common prefix has no common ancestor: [%s]", commonPrefix)));

        if (!(commonAncestor instanceof Free)) {
            throw new IllegalStateException(
                    format("Common ancestor of a common prefix must be a free node.\nCommon prefix: %s\nAncestor: %s",
                           commonPrefix, commonAncestor));
        }

        var newModel = model;

        // A pre-common-ancestor of N is a child of `commonAncestor` on the path between N and `commonAncestor`.
        // I.e., node U_i, where `parent(U_i) = commonAncestor` and `ancestors(N_i)` contains `U_i`.
        final List<GrammarNode> preCommonAncestors;
        {
            var newModelRef = new AtomicReference<>(newModel);
            preCommonAncestors = commonPrefix
                    .stream()
                    .map(node -> {
                        var path = dropRight(pathToAncestor(model, node, commonAncestor).get(), 1);
                        if (path.isEmpty()) {
                            // When parent(node) == commonAncestor, introduce a new full node between `node` and `commonAncestor`.
                            // The semantics of this new node is the identity function applied to `node`.
                            var interNode = nodeFactory.newNode();
                            newModelRef.set(newModelRef.get()
                                                       .addNode(interNode)
                                                       .set(interNode, PARENT, commonAncestor)
                                                       .set(node, PARENT, interNode));
                            return interNode;
                        }
                        else {
                            return path.getLast();
                        }
                    })
                    .toList();
            newModel = newModelRef.get();
        }

        // For each common prefix node N:
        // * disconnect N from its parent P;
        // * make parent(next(N)) = P.
        // If P is the common ancestor, introduce a new full node X as a parent of N and make parent(X) = P.
        newModel = foldl((accModel, node) -> {
                             final GrammarNode nodeParent;
                             if (accModel.getF(node, PARENT).equals(commonAncestor)) {
                                 nodeParent = nodeFactory.newNode();
                                 accModel = accModel.set(nodeParent, PARENT, commonAncestor);
                             }
                             else {
                                 nodeParent = accModel.getF(node, PARENT);
                             }
                             var maybeNodeNext = accModel.get(node, NEXT);
                             return accModel
                                     .removeAttribute(PARENT, node)
                                     .set(maybeNodeNext, PARENT, nodeParent)
                                     .removeAttribute(NEXT, node);
                         },
                         newModel,
                         commonPrefix);

        // We only need one common prefix node, since we are eliminating the common prefix.
        var commonPrefixNode = first(commonPrefix);
        newModel = newModel.removeNodes(remove(commonPrefix, commonPrefixNode));

        // Add a new child to the common ancestor, a full node whose child is the common prefix node.
        var $1 = nodeFactory.newNode();
        newModel = newModel
                .addNode($1)
                .set(commonPrefixNode, PARENT, $1)
                .set($1, PARENT, commonAncestor);

        // Next node of the common prefix node.
        // Its children are the pre-common-ancestors.
        var $2 = nodeFactory.newFreeNode();
        newModel = newModel
                .addNode($2)
                .set(commonPrefixNode, NEXT, $2)
                .setAll(preCommonAncestors.stream().map(node -> t3(node, PARENT, $2)));

        return newModel;
    }

    public static Set<GrammarNode> findCommonPrefix(GrammarTreeModel model, Function<? super GrammarNode, ?> nodeClassifier) {
        return findCommonPrefix(model, model.root(), nodeClassifier);
    }

    public static Set<GrammarNode> findCommonPrefix(GrammarTreeModel model, GrammarNode node, Function<? super GrammarNode, ?> nodeClassifier) {
        var allPaths = generatePaths(model, node).toList();

        // A group is a set of paths that share a common prefix symbol.
        // A group is a set of pairs (path, common prefix symbol index)
        Collection<List<T2<List<GrammarNode>, Integer>>> groups =
                generatePairs(allPaths)
                        .map(pathPair -> pathPair
                                .map((path1, path2) -> findCommonPrefix(path1, path2, nodeClassifier))
                                .map(idxPair -> combinePairs(pathPair, idxPair)))
                        .flatMap(Optional::stream)
                        .flatMap(T2::stream)
                        .collect(groupingBy(T2._map((path, idx) -> nodeClassifier.apply(path.get(idx))),
                                            toList()))
                        .values();

        if (groups.isEmpty()) {
            return Set.of();
        }

        // Efficient lookup of common prefix symbol indexes.
        // Key: path.
        // Value: indexes of all common prefix symbols that occur in this path (may span several groups).
        Map<List<GrammarNode>, List<Integer>> pathIndexes = groups
                .stream()
                .flatMap(List::stream)
                .collect(groupingBy(T2::fst, mapping(T2::snd, toList())));

        // Choose a group that has a subset of paths in which no path has an earlier common prefix symbol.
        // I.e., the shortest distance to a common prefix symbol.

        List<T2<List<GrammarNode>, Integer>> chosenGroup = groups
                .stream()
                .map(group -> {
                    var paths = group.stream()
                                     .filter(pair -> pair.map((path, idx) -> isMinIndex(path, idx, pathIndexes)))
                                     .toList();
                    return Optional.of(paths).filter(it -> it.size() > 1);
                })
                .flatMap(Optional::stream)
                .findFirst()
                // There must exist such a group.
                .orElseThrow(() -> new IllegalStateException("No group with shortest distance to a common prefix symbol."));

        if (chosenGroup.size() < 2) {
            throw new IllegalStateException(format("Invalid common prefix group: %s", chosenGroup));
        }

        var result = chosenGroup.stream()
                .map(pair -> pair.map(List::get))
                .collect(toSet());

        if (!allElementsEqual(result, nodeClassifier)) {
            throw new IllegalStateException(format("Invalid common prefix. All nodes must be the same.\nCommon prefix: %s", result));
        }

        return result;
    }

    static Optional<T2<Integer, Integer>> findCommonPrefix(
            List<GrammarNode> path1,
            List<GrammarNode> path2,
            Function<? super GrammarNode, ?> nodeClassifier)
    {
        BiPredicate<GrammarNode, GrammarNode> areNodesEqual = (node1, node2) -> nodeClassifier.apply(node1).equals(nodeClassifier.apply(node2));

        var sharedPrefixSize = (int) zip(path1, path2)
                .takeWhile(pair -> pair.test(areNodesEqual))
                .count();

        var newPath1 = dropLeft(path1, sharedPrefixSize);
        var newPath2 = dropLeft(path2, sharedPrefixSize);

        return indexOfFirstCommonElement(newPath1, newPath2, areNodesEqual)
                .map(pair -> pair.map((i1, i2) -> t2(i1 + sharedPrefixSize, i2 + sharedPrefixSize)));
    }

    static <X> Optional<T2<Integer, Integer>> indexOfFirstCommonElement(
            List<? extends X> path1,
            List<? extends X> path2,
            BiPredicate<? super X, ? super X> test)
    {
        return generatePairs(enumeratedStream(path1.stream(), T2::t2).toList(),
                             enumeratedStream(path2.stream(), T2::t2).toList())
                .filter(pairs -> pairs.test((p1, p2) -> test.test(p1.fst(), p2.fst())))
                .map(pairs -> pairs.map((p1, p2) -> t2(p1.snd(), p2.snd())))
                .findFirst();
    }

    static <A, B, C, D> T2<T2<A, C>, T2<B, D>> combinePairs(T2<A, B> pair1, T2<C, D> pair2) {
        return t2(t2(pair1.fst(), pair2.fst()), t2(pair1.snd(), pair2.snd()));
    }

    private static Stream<T3<GrammarNode, List<GrammarNode>, Integer>> indexPath(List<GrammarNode> path) {
        return enumeratedStream(path.stream(), (node, i) -> t3(node, path, i));
    }

    private static boolean isMinIndex(List<GrammarNode> path, Integer idx, Map<List<GrammarNode>, ? extends Collection<Integer>> indexes) {
        return indexes.get(path).stream().min(Integer::compareTo).get().intValue() == idx;
    }

    static Stream<List<GrammarNode>> generatePaths(GrammarTreeModel model) {
        var children = model.get(model.root(), CHILDREN).orElseGet(Set::of);
        return children.stream()
                .flatMap(c -> generatePaths(model, c));
    }

    static Stream<List<GrammarNode>> generatePaths(GrammarTreeModel model, GrammarNode node) {
        if (node.equals(model.root())) {
            return generatePaths(model);
        }
        else {
            var children = model.get(node, CHILDREN).orElseGet(Set::of);
            return children.isEmpty()
                    ? Stream.of(List.of(node))
                    : children.stream()
                            .flatMap(c -> generatePaths(model, c))
                            .map(path -> cons(node, path));
        }
    }

    static <X> Stream<T2<X, X>> generatePairs(List<X> xs) {
        return zipWith(xs.stream(), dropLeft(tails(xs), 1), (hd, tl) -> tl.stream().map(tlElt -> t2(hd, tlElt)))
                .flatMap(identity());
    }

    static <X, Y> Stream<T2<X, Y>> generatePairs(List<X> xs, List<Y> ys) {
        return xs.stream()
                .flatMap(x -> ys.stream().map(y -> t2(x, y)));
    }

    static Optional<GrammarNode> commonAncestor(GrammarTreeModel model, GrammarNode node1, GrammarNode node2) {
        var ancestors1 = ancestors(model, node1).collect(toSet());
        return ancestors(model, node2)
                .filter(ancestors1::contains)
                .findFirst();
    }

    static Optional<GrammarNode> commonAncestor(GrammarTreeModel model, Collection<GrammarNode> nodes) {
        if (nodes.size() < 2) {
            return Optional.empty();
        }
        else {
            var nodesList = List.copyOf(nodes);
            return foldl((acc, node) -> acc.flatMap(anc -> isAncestor(model, anc, node) ? Optional.of(anc) : commonAncestor(model, anc, node)),
                         commonAncestor(model, nodesList.get(0), nodesList.get(1)),
                         dropLeft(nodesList, 2));
        }
    }

    /**
     * Path from child to ancestor, including the ancestor.
     * If child equals ancestor, an empty path.
     */
    static Optional<List<GrammarNode>> pathToAncestor(GrammarTreeModel model, GrammarNode child, GrammarNode ancestor) {
        if (child.equals(ancestor)) {
            return Optional.of(List.of());
        }

        final var path = foldMaybe((ancestors, node) -> !ancestors.isEmpty() && ancestors.getFirst().equals(ancestor)
                                           ? Optional.empty()
                                           : Optional.of(cons(node, ancestors)),
                                   List.<GrammarNode>of(),
                                   ancestors(model, child))
                .reversed();

        if (!path.isEmpty() && path.getLast().equals(ancestor)) {
            return Optional.of(path);
        }
        else {
            return Optional.empty();
        }
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private CommonPrefix() {}

}
