package compojar.model;

import compojar.util.T2;
import compojar.util.T3;
import compojar.util.Util;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static compojar.model.Eq.eqMapAt;
import static compojar.model.Eq.eqOn;
import static compojar.model.Keys.*;
import static compojar.util.T2.t2;
import static compojar.util.T3.t3;
import static compojar.util.Util.*;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public record GrammarTreeModel (
        Set<GrammarNode> nodes,
        GrammarNode root,
        Map<GrammarNode, Map<Key, Object>> attributes
) {

    public GrammarTreeModel {
        if (!nodes.contains(root)) {
            throw new IllegalArgumentException("Root node is not in the tree.");
        }
    }

    public GrammarTreeModel(Set<GrammarNode> nodes, GrammarNode root) {
        this(nodes, root, Map.of());
    }

    /**
     * Returns an Eq that compares all nodes on attribute values associated with the specified key.
     */
    public static <V> Eq<GrammarTreeModel> eqAttribute(Key<V> key, Eq<V> eqVal) {
        return eqOn(GrammarTreeModel::attributes, (Eq) eqMapAt(key, eqVal));
    }

    public Optional<GrammarNode> findNode(Predicate<? super GrammarNode> predicate) {
        return nodes.stream().filter(predicate).findFirst();
    }

    public <X> GrammarTreeModel maybeMap(Optional<X> maybeX, BiFunction<? super X, GrammarTreeModel, GrammarTreeModel> fn) {
        return maybeX.map(x -> fn.apply(x, this)).orElse(this);
    }

    /**
     * Not to be used directly. Use {@link Key#set(GrammarTreeModel, GrammarNode, Object)}.
     * <p>
     * Implementations of {@link Key#set(GrammarTreeModel, GrammarNode, Object)} can use this method.
     */
    <V> GrammarTreeModel _setAttribute(GrammarNode node, Key<V> key, V attribute) {
        var maybeOldValue = Optional.ofNullable((V) attributes.getOrDefault(node, Map.of()).get(key));
        return key._set(this, node, attribute, maybeOldValue);
    }

    <V> GrammarTreeModel __setAttribute(GrammarNode node, Key<V> key, V value) {
        return new GrammarTreeModel(nodes,
                                    root,
                                    insertWith(attributes, node, Map.of(key, value), Util::mapUnionRight));
    }

    public <V> GrammarTreeModel set(GrammarNode node, Key<V> key, V attribute) {
        assertContains(node);
        return key.set(this, node, attribute);
    }

    public <V> GrammarTreeModel set(Optional<GrammarNode> node, Key<V> key, V attribute) {
        return node.map(next -> set(next, key, attribute)).orElse(this);
    }

    public <V> GrammarTreeModel maybeSet(GrammarNode node, Key<V> key, Optional<V> maybeAttribute) {
        return maybeAttribute.map(a -> set(node, key, a)).orElse(this);
    }

    public <V> GrammarTreeModel set(T3<GrammarNode, Key<V>, V> triple) {
        assertContains(triple.fst());
        return triple.map((node, key, attribute) -> key.set(this, node, attribute));
    }

    /**
     * @param triples  {@code (node, key, attribute)}
     */
    public GrammarTreeModel setAll (Stream<? extends T3> triples) {
        return foldl((acc, _3) -> (GrammarTreeModel) _3.map((node, key, attr) -> acc.set((GrammarNode) node, (Key) key, attr)),
                     this,
                     triples);
    }

    public GrammarTreeModel setAll (Collection<? extends T3> triples) {
        return setAll(triples.stream());
    }

    public GrammarTreeModel removeNode(GrammarNode node) {
        assertContains(node);

        if (node.equals(root)) {
            throw new IllegalArgumentException("Root node can't be removed.");
        }

        // TODO Dependency graph of attributes should be taken into account.
        var model = foldl((accModel, pair) -> pair.map((key, attr) -> accModel.removeAttribute(key, node)),
                          this,
                          streamAttributes(node));

        // TODO Consider if it makes more sense to remove attributes after this.
        model = foldl((accModel, _3) -> _3.map((nd, key, attr) -> key.removeNode(accModel, nd, attr, node)),
                      model,
                      streamAttributes());

        model = new GrammarTreeModel(remove(nodes, node), root, mapRemove(model.attributes, node));

        return model;
   }

    public GrammarTreeModel removeNode(Optional<GrammarNode> maybeNode) {
        return maybeNode.map(this::removeNode).orElse(this);
    }

    public GrammarTreeModel removeNodes(GrammarNode... nodes) {
        return removeNodes(Arrays.stream(nodes));
    }

    public GrammarTreeModel removeNodes(Collection<GrammarNode> nodes) {
        return removeNodes(nodes.stream());
    }

    public GrammarTreeModel removeNodes(Stream<GrammarNode> nodes) {
        return foldl(GrammarTreeModel::removeNode,
                     this,
                     nodes);
    }

   private Stream<T3<GrammarNode, Key, Object>> streamAttributes() {
        return attributes.entrySet()
                .stream()
                .flatMap(entry -> {
                    final var node = entry.getKey();
                    return entry.getValue().entrySet()
                            .stream()
                            .map(key_val -> t3(node, key_val.getKey(), key_val.getValue()));
                });
   }

    private Stream<T2<Key, Object>> streamAttributes(GrammarNode node) {
        assertContains(node);

        return stream(attributes.getOrDefault(node, Map.of()), T2::t2);
    }

    public <V> GrammarTreeModel setAttributeWith(GrammarNode node, Key<V> key, V attribute, BinaryOperator<V> merge) {
        assertContains(node);

        return set(node, key, get(node, key).map(a -> merge.apply(a, attribute)).orElse(attribute));
    }

    public <V> GrammarTreeModel updateAttribute(GrammarNode node, Key<V> key, Function<? super V, ? extends V> fn) {
        assertContains(node);

        return get(node, key)
                .map(attr -> set(node, key, fn.apply(attr)))
                .orElse(this);
    }

    public GrammarTreeModel addAttributes(Map<GrammarNode, Map<Key, Object>> attributes) {
        attributes.keySet().forEach(this::assertContains);

        return foldl((acc, node, map) -> foldl((acc2, key, attr) -> key.set(acc2, node, attr),
                                               acc,
                                               map),
                     this,
                     attributes);
    }

    public GrammarTreeModel copyAttributes(GrammarNode from, GrammarNode to) {
        assertContainsAll(from, to);

        var attributesToCopy = filterKeys(attributes.getOrDefault(from, Map.of()),
                                          (key, attr) -> key.canCopy(this, from, to, attr));
        return addAttributes(Map.of(to, attributesToCopy));
    }

    public GrammarTreeModel copyAttributes(GrammarNode from, GrammarNode to, Key... keys) {
        return copyAttributes(from, to, Set.of(keys));
    }

    public GrammarTreeModel copyAttributes(GrammarNode from, GrammarNode to, Collection<? extends Key> keys) {
        assertContainsAll(from, to);

        keys.forEach(key -> get(from, key).ifPresent(attr -> {
            if (!key.canCopy(this, from, to, attr)) {
                throw new IllegalStateException(format("Attribute for key [%s] cannot be copied from [%s] to [%s].",
                                                       key, from, to));
            }
        }));

        var keySet = Set.copyOf(keys);
        var attributesToCopy = filterKeys(attributes.getOrDefault(from, Map.of()),
                                          (key, attr) -> keySet.contains(key) && key.canCopy(this, from, to, attr));
        return addAttributes(Map.of(to, attributesToCopy));
    }

    public T2<GrammarTreeModel, GrammarNode> addCopyOf(GrammarNode node, Key... keys) {
        assertContains(node);

        var copy = node.copy();
        return t2(addNode(copy).copyAttributes(node, copy, keys), copy);
    }

    public T2<GrammarTreeModel, List<GrammarNode>> addCopiesOf(Collection<? extends GrammarNode> nodes, Key... keys) {
        assertContainsAll(nodes);

        return foldl2((acc, copies, node) -> {
                          var copy = node.copy();
                          return t2(acc.addNode(copy).copyAttributes(node, copy, keys), cons(copy, copies));
                      },
                      this, List.<GrammarNode>of(),
                      nodes.stream())
                .map2(List::reversed);
    }

    <V> Optional<V> _getAttribute(GrammarNode node, Key<V> key) {
        return Optional.ofNullable((V) attributes.getOrDefault(node, Map.of()).get(key));
    }

    public <V> Optional<V> get(GrammarNode node, Key<V> key) {
        assertContains(node);

        return _getAttribute(node, key);
    }

    public <V> V getF(GrammarNode node, Key<V> key) {
        assertContains(node);

        return requireAttribute(node, key);
    }

    public <V> V requireAttribute(GrammarNode node, Key<V> key) {
        return get(node, key)
                .orElseThrow(() -> new IllegalStateException(format("No attribute for key [%s] in node [%s]", key, node)));
    }

    public boolean has(GrammarNode node, Key<?> key) {
        return attributes.getOrDefault(node, Map.of()).containsKey(key);
    }

    public GrammarNode assertContains(GrammarNode node) {
        if (!nodes.contains(node)) {
            throw new IllegalStateException(format("Foreign node: %s", node));
        }
        return node;
    }

    public void assertContainsAll(GrammarNode... nodes) {
        for (GrammarNode node : nodes) {
            assertContains(node);
        }
    }

    public void assertContainsAll(Iterable<? extends GrammarNode> nodes) {
        for (GrammarNode node : nodes) {
            assertContains(node);
        }
    }

    public GrammarTreeModel addNodes(Collection<? extends GrammarNode> nodes) {
        return new GrammarTreeModel(concatSet(this.nodes, nodes),
                                    root,
                                    attributes);
    }

    public GrammarTreeModel addNodes(GrammarNode... nodes) {
        return addNodes(Arrays.asList(nodes));
    }

    public GrammarTreeModel addNode(GrammarNode node) {
        return addNodes(Set.of(node));
    }

    public <V> GrammarTreeModel removeAttribute(Key<V> key, GrammarNode node) {
        return get(node, key)
                .map(attr -> key._remove(this, node, attr))
                .orElse(this)
                ._removeAttribute(key, node);
    }

    private <V> GrammarTreeModel _removeAttribute(Key<V> key, GrammarNode node) {
        return new GrammarTreeModel(nodes,
                                    root,
                                    update(attributes, node, map -> mapRemove(map, key)));
    }

    /**
     * Replace all occurences of a node.
     */
    public GrammarTreeModel replaceNode(GrammarNode oldNode, GrammarNode newNode) {
        assertContains(oldNode);

        // Adjust attributes with both old and new nodes in the model to avoid "foreign node" errors.
        var newModel = foldl((acc, t3) -> t3.map((node, key, attr) -> key.replaceNode(acc, node, attr, oldNode, newNode)),
                             this.addNode(newNode),
                             streamAttributes());

        return new GrammarTreeModel(
                replace(newModel.nodes, oldNode, newNode),
                newModel.root.equals(oldNode) ? newNode : newModel.root,
                replaceKey(newModel.attributes, oldNode, newNode));
    }

    public GrammarTreeModel subtree(GrammarNode subRoot) {
        assertContains(subRoot);

        if (root.equals(subRoot)) {
            return this;
        } else {
            // Next nodes of subRoot are excluded.
            var subNodes = searchNodes(subRoot, nd -> Stream.concat(get(nd, CHILDREN).orElseGet(Set::of).stream(),
                                                                    nd.equals(subRoot) ? Stream.of() : get(nd, NEXT).stream()));
            return new GrammarTreeModel(subNodes,
                                        subRoot,
                                        filterKeys(attributes, (k, $) -> subNodes.contains(k)))
                    // Low-level operations because the new tree does not contain these nodes anymore.
                    ._removeAttribute(PARENT, subRoot)
                    ._removeAttribute(NEXT, subRoot)
                    ._removeAttribute(TARGET, subRoot);
        }
    }

    private Set<GrammarNode> searchNodes(GrammarNode node, Function<? super GrammarNode, Stream<? extends GrammarNode>> fn) {
        return searchNodes(Set.of(node), fn, Set.of());
    }

    private Set<GrammarNode> searchNodes(
            Set<GrammarNode> unvisited,
            Function<? super GrammarNode, Stream<? extends GrammarNode>> fn,
            Set<GrammarNode> acc)
    {
        return unvisited.isEmpty()
                ? acc
                : searchNodes(unvisited.stream().flatMap(fn).collect(toSet()),
                              fn,
                              concatSet(unvisited, acc));
    }

    public GrammarTreeModel include(GrammarTreeModel model) {
        if (!intersection(nodes, model.nodes()).isEmpty()) {
            throw new IllegalArgumentException("Cannot include a model that shares nodes with this one.");
        }

        return new GrammarTreeModel(concatSet(nodes, model.nodes()),
                                    root,
                                    mapStrictMerge(attributes, model.attributes()));
    }

    public GrammarTreeModel setRoot(GrammarNode newRoot) {
        return replaceNode(this.root, newRoot);
    }

    private GrammarTreeModel removeTargetAttributes(GrammarNode target) {
        var links = linksTo(this, target).toList();
        return foldl((m, ln) -> m.removeAttribute(TARGET, ln), this, links);
    }

    public GrammarTreeModel pruneSubtreeWithNext(GrammarNode node) {
        var result = get(node, NEXT).map(this::pruneSubtreeWithNext).orElse(this);

        result = foldl(GrammarTreeModel::pruneSubtreeWithNext,
                       result.removeTargetAttributes(node),
                       get(node, CHILDREN).orElseGet(Set::of));

        return result.removeNode(node);
    }

    public GrammarTreeModel pruneSubtree(GrammarNode node) {
        // First remove all links to `node` so that the subtree can be removed.
        var result = foldl(GrammarTreeModel::pruneSubtreeWithNext,
                           removeTargetAttributes(node),
                           get(node, CHILDREN).orElseGet(Set::of));

        return result.removeNode(node);
    }

    public GrammarTreeModel replaceSubtree(GrammarNode node, GrammarTreeModel subModel) {
        var maybeNext = get(node, NEXT);
        var maybeParent = get(node, PARENT);

        return pruneSubtree(node)
                .include(subModel)
                .maybeSet(subModel.root(), PARENT, maybeParent)
                .maybeSet(subModel.root(), NEXT, maybeNext);
    }

    public GrammarTreeModel retainNodes(Set<? extends GrammarNode> nodes) {
        assertContainsAll(nodes);

        return removeNodes(difference(this.nodes, nodes));
    }

    public GrammarTreeModel copy() {
        var newNodes = nodes.stream().collect(toMap(Function.identity(), GrammarNode::copy));

        var model = foldl((acc, oldNode, newNode) -> acc.replaceNode(oldNode, newNode),
                          this,
                          newNodes);

        return new GrammarTreeModel(unmodifiableSet(new HashSet<>(newNodes.values())),
                                    newNodes.get(root),
                                    foldl((acc, oldNode, newNode) -> replaceKey(acc, oldNode, newNode),
                                          model.attributes(),
                                          newNodes));
    }

    /**
     * Removes the sub-tree rooted at the specified node.
     * The node itself is not removed.
     */
    public GrammarTreeModel removeSubtreeBelow(GrammarNode node) {
        return removeNodes(remove(subtree(node).nodes(), node));
    }

    public GrammarTreeModel pipe(Function<? super GrammarTreeModel, GrammarTreeModel> fn) {
        return fn.apply(this);
    }

    public boolean contains(GrammarNode node) {
        return nodes.contains(node);
    }

    // public <V> GrammarTreeModel removeAttributesOnValue(Key<V> key, V attribute) {
    //     return new GrammarTreeModel(nodes,
    //                                 root,
    //                                 mapValues(attributes, map -> filterValues(map, v -> !v.equals(attribute))));
    // }

}
