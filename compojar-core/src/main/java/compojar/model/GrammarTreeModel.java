package compojar.model;

import compojar.util.T2;
import compojar.util.T3;
import compojar.util.Util;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

import static compojar.util.T3.t3;
import static compojar.util.Util.*;
import static java.lang.String.format;

public record GrammarTreeModel (
        Set<GrammarTree> nodes,
        GrammarTree root,
        Map<GrammarTree, Map<Key, Object>> attributes
) {

    public GrammarTreeModel {
        if (!nodes.contains(root)) {
            throw new IllegalArgumentException("Root node is not in the tree.");
        }
    }

    public GrammarTreeModel (Set<GrammarTree> nodes, GrammarTree root) {
        this(nodes, root, Map.of());
    }

    /**
     * Not to be used directly. Use {@link Key#set(GrammarTreeModel, GrammarTree, Object)}.
     * <p>
     * Implementations of {@link Key#set(GrammarTreeModel, GrammarTree, Object)} can use this method.
     */
    <V> GrammarTreeModel _setAttribute(GrammarTree node, Key<V> key, V attribute) {
        var maybeOldValue = Optional.ofNullable((V) attributes.getOrDefault(node, Map.of()).get(key));
        return key._set(this, node, attribute, maybeOldValue);
    }

    <V> GrammarTreeModel __setAttribute(GrammarTree node, Key<V> key, V value) {
        return new GrammarTreeModel(nodes,
                                    root,
                                    insertWith(attributes, node, Map.of(key, value), Util::mapUnionRight));
    }

    public <V> GrammarTreeModel set (GrammarTree node, Key<V> key, V attribute) {
        assertContains(node);
        return key.set(this, node, attribute);
    }

    public <V> GrammarTreeModel set (Optional<GrammarTree> node, Key<V> key, V attribute) {
        return node.map(next -> set(next, key, attribute)).orElse(this);
    }

    public <V> GrammarTreeModel set (T3<GrammarTree, Key<V>, V> triple) {
        assertContains(triple.fst());
        return triple.map((node, key, attribute) -> key.set(this, node, attribute));
    }

    /**
     * @param triples  {@code (node, key, attribute)}
     */
    public GrammarTreeModel setAll (Stream<? extends T3> triples) {
        return foldl((acc, _3) -> (GrammarTreeModel) _3.map((node, key, attr) -> acc.set((GrammarTree) node, (Key) key, attr)),
                     this,
                     triples);
    }

    public GrammarTreeModel setAll (Collection<? extends T3> triples) {
        return setAll(triples.stream());
    }

    public GrammarTreeModel removeNode (GrammarTree node) {
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

    public GrammarTreeModel removeNodes (GrammarTree... nodes) {
        return removeNodes(Arrays.stream(nodes));
    }

    public GrammarTreeModel removeNodes (Collection<GrammarTree> nodes) {
        return removeNodes(nodes.stream());
    }

    public GrammarTreeModel removeNodes (Stream<GrammarTree> nodes) {
        return foldl(GrammarTreeModel::removeNode,
                     this,
                     nodes);
    }

   private Stream<T3<GrammarTree, Key, Object>> streamAttributes() {
        return attributes.entrySet()
                .stream()
                .flatMap(entry -> {
                    final var node = entry.getKey();
                    return entry.getValue().entrySet()
                            .stream()
                            .map(key_val -> t3(node, key_val.getKey(), key_val.getValue()));
                });
   }

    private Stream<T2<Key, Object>> streamAttributes(GrammarTree node) {
        assertContains(node);

        return stream(attributes.getOrDefault(node, Map.of()), T2::t2);
    }

    public <V> GrammarTreeModel setAttributeWith(GrammarTree node, Key<V> key, V attribute, BinaryOperator<V> merge) {
        assertContains(node);

        return set(node, key, get(node, key).map(a -> merge.apply(a, attribute)).orElse(attribute));
    }

    public <V> GrammarTreeModel updateAttribute(GrammarTree node, Key<V> key, Function<? super V, ? extends V> fn) {
        assertContains(node);

        return get(node, key)
                .map(attr -> set(node, key, fn.apply(attr)))
                .orElse(this);
    }

    public GrammarTreeModel addAttributes (Map<GrammarTree, Map<Key, Object>> attributes) {
        attributes.keySet().forEach(this::assertContains);

        return foldl((acc, node, map) -> foldl((acc2, key, attr) -> key.set(acc2, node, attr),
                                               acc,
                                               map),
                     this,
                     attributes);
    }

    public GrammarTreeModel copyAttributes (GrammarTree from, GrammarTree to) {
        assertContainsAll(from, to);

        var attributesToCopy = filterKeys(attributes.getOrDefault(from, Map.of()),
                                          (key, attr) -> key.canCopy(this, from, to, attr));
        return addAttributes(Map.of(to, attributesToCopy));
    }

    <V> Optional<V> _getAttribute(GrammarTree node, Key<V> key) {
        return Optional.ofNullable((V) attributes.getOrDefault(node, Map.of()).get(key));
    }

    public <V> Optional<V> get (GrammarTree node, Key<V> key) {
        assertContains(node);

        return _getAttribute(node, key);
    }

    public <V> V getF (GrammarTree node, Key<V> key) {
        assertContains(node);

        return requireAttribute(node, key);
    }

    public <V> V requireAttribute (GrammarTree node, Key<V> key) {
        return get(node, key)
                .orElseThrow(() -> new IllegalStateException(format("No attribute for key [%s] in node [%s]", key, node)));
    }

    public boolean has (GrammarTree node, Key<?> key) {
        return attributes.getOrDefault(node, Map.of()).containsKey(key);
    }

    public GrammarTree assertContains (GrammarTree node) {
        if (!nodes.contains(node)) {
            throw new IllegalStateException(format("Foreign node: %s", node));
        }
        return node;
    }

    public void assertContainsAll (GrammarTree... nodes) {
        for (GrammarTree node : nodes) {
            assertContains(node);
        }
    }

    public GrammarTreeModel addNodes(Collection<? extends GrammarTree> nodes) {
        return new GrammarTreeModel(concatSet(this.nodes, nodes),
                                    root,
                                    attributes);
    }

    public GrammarTreeModel addNode(GrammarTree node) {
        return addNodes(Set.of(node));
    }

    public <V> GrammarTreeModel removeAttribute(Key<V> key, GrammarTree node) {
        return get(node, key)
                .map(attr -> key._remove(this, node, attr))
                .orElse(this)
                ._removeAttribute(key, node);
    }

    private <V> GrammarTreeModel _removeAttribute(Key<V> key, GrammarTree node) {
        return new GrammarTreeModel(nodes,
                                    root,
                                    update(attributes, node, map -> mapRemove(map, key)));
    }


    // public <V> GrammarTreeModel removeAttributesOnValue(Key<V> key, V attribute) {
    //     return new GrammarTreeModel(nodes,
    //                                 root,
    //                                 mapValues(attributes, map -> filterValues(map, v -> !v.equals(attribute))));
    // }

}
