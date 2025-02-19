package compojar.model;

import compojar.model.GrammarTree.FreeNode;
import compojar.model.GrammarTree.Node;

public interface NodeFactory {

    FreeNode newFreeNode();

    FreeNode newFreeNode(CharSequence nameHint);

    Node newNode();

    Node newNode(CharSequence nameHint);

}
