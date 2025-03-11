package compojar.model;

import compojar.model.GrammarNode.Free;
import compojar.model.GrammarNode.Full;

public interface NodeFactory {

    Free newFreeNode();

    Free newFreeNode(CharSequence nameHint);

    Full newNode();

    Full newNode(CharSequence nameHint);

}
