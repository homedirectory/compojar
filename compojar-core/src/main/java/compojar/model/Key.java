package compojar.model;

import java.util.Optional;

public abstract class Key<V> {

    /**
     * Reads the attribute value.
     * Public API.
     */
    protected final Optional<V> get (GrammarTreeModel model, GrammarTree node) {
        return model._getAttribute(node, this);
    }

    /**
     * Sets the attribute value.
     * Public API.
     */
    protected final GrammarTreeModel set (GrammarTreeModel model, GrammarTree node, V attribute) {
        return model._setAttribute(node, this, attribute);
    }

    /**
     * Sets the attribute value.
     * This method is the only way of persisting attribute values.
     * Therefore, overriding methods should call this method (via {@code super}) to persist the attribute value.
     */
    protected GrammarTreeModel _set (GrammarTreeModel model, GrammarTree node, V newValue, Optional<V> maybeOldValue) {
        return model.__setAttribute(node, this, newValue);
    }

    /**
     * Called before an attribute is removed.
     *
     * @param node  the node whose attribute is being removed
     * @param value  current attribute value
     */
    protected GrammarTreeModel _remove(GrammarTreeModel model, GrammarTree node, V value) {
        return model;
    }

    /**
     * Called before a node is removed, after its attributes are removed.
     * Effectively, through this method all existing attributes are informed when a node is removed.
     * <p>
     * Implementations don't need to handle the case when {@code node == removedNode}, because
     * {@link #_remove(GrammarTreeModel, GrammarTree, Object)} will be called to handle that.
     *
     * @param node  node bearing the attribute
     * @param attribute  attribute value
     * @param removedNode  node being removed
     */
    protected GrammarTreeModel removeNode(GrammarTreeModel model, GrammarTree node, V attribute, GrammarTree removedNode) {
        return model;
    }

    /**
     * Determines whether an attribute is copied as a result of {@link GrammarTreeModel#copyAttributes(GrammarTree, GrammarTree)}.
     *
     * @param attribute  value of the attribute associated with node {@code from}
     */
    protected boolean canCopy (GrammarTreeModel model, GrammarTree from, GrammarTree to, V attribute) {
        return true;
    }

}
