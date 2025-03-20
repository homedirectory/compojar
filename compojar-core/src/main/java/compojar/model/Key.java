package compojar.model;

import java.util.Optional;

public abstract class Key<V> {

    /**
     * Reads the attribute value.
     * Public API.
     */
    protected final Optional<V> get(GrammarTreeModel model, GrammarNode node) {
        return model._getAttribute(node, this);
    }

    /**
     * Sets the attribute value.
     * Public API.
     */
    protected final GrammarTreeModel set(GrammarTreeModel model, GrammarNode node, V attribute) {
        return model._setAttribute(node, this, attribute);
    }

    /**
     * Sets the attribute value.
     * This method is the only way of persisting attribute values.
     * Therefore, overriding methods should call this method (via {@code super}) to persist the attribute value.
     */
    protected GrammarTreeModel _set(GrammarTreeModel model, GrammarNode node, V newValue, Optional<V> maybeOldValue) {
        return model.__setAttribute(node, this, newValue);
    }

    /**
     * Called before an attribute is removed.
     *
     * @param node  the node whose attribute is being removed
     * @param value  current attribute value
     */
    protected GrammarTreeModel _remove(GrammarTreeModel model, GrammarNode node, V value) {
        return model;
    }

    /**
     * Called before a node is removed, after its attributes are removed.
     * Effectively, through this method all existing attributes are informed when a node is removed.
     * <p>
     * Implementations don't need to handle the case when {@code node == removedNode}, because
     * {@link #_remove(GrammarTreeModel, GrammarNode, Object)} will be called to handle that.
     *
     * @param node  node bearing the attribute
     * @param attribute  attribute value
     * @param removedNode  node being removed
     */
    protected GrammarTreeModel removeNode(GrammarTreeModel model, GrammarNode node, V attribute, GrammarNode removedNode) {
        return model;
    }

    /**
     * Determines whether an attribute is copied as a result of {@link GrammarTreeModel#copyAttributes(GrammarNode, GrammarNode)}.
     *
     * @param attribute  value of the attribute associated with node {@code from}
     */
    protected boolean canCopy(GrammarTreeModel model, GrammarNode from, GrammarNode to, V attribute) {
        return true;
    }

    /**
     * Called before a node is replaced.
     * When this method is called, both the old and new nodes are already present in the model.
     * Implementations should operate on a low level, through {@link #_set(GrammarTreeModel, GrammarNode, Object, Optional)}.
     * The sole effect of the replacement should be the replacement of references to {@code oldNode} by references to {@code newNode}.
     *
     * @param node  node bearing the attribute
     * @param attribute  attribute value
     * @param oldNode  node being replaced
     * @param newNode  the replacement node
     */
    protected GrammarTreeModel replaceNode(GrammarTreeModel model, GrammarNode node, V attribute, GrammarNode oldNode, GrammarNode newNode) {
        return model;
    }

    protected Validation.Result validate(GrammarTreeModel model, GrammarNode node, V attribute) {
        return Validation.Result.ok();
    }

}
