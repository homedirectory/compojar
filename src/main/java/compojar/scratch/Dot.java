package compojar.scratch;

import compojar.bnf.BNF;
import compojar.bnf.Terminal;
import compojar.bnf.Variable;
import compojar.gen.Generator;
import compojar.gen.Namer;

import java.nio.file.Path;
import java.util.Set;

import static compojar.bnf.Rule.derivation;
import static compojar.bnf.Rule.selection;
import static compojar.scratch.Dot.T.*;
import static compojar.scratch.Dot.V.*;

public interface Dot {

    public static void main(String[] args) {
        var namer = new Namer("Dot", "dot");
        var g = new Generator(namer, bnf);
        g.generate(Path.of("src/test/generated-sources/"));
    }

    enum T implements Terminal {
        edge, to, $, node
    }

    enum V implements Variable {
        Graph,
        Nodes,
        Edges, NodesNil, NodesCons, Node, EdgesCons, EdgesNil, Edge,
    }

    BNF bnf = new BNF(
            Set.of(derivation(Graph, Nodes, Edges),
                   selection(Nodes, NodesCons, NodesNil),
                   derivation(NodesCons, Node, Nodes),
                   derivation(NodesNil, $),
                   derivation(Node, node.parameters(String.class, "name")),
                   selection(Edges, EdgesCons, EdgesNil),
                   derivation(EdgesCons, Edge, Edges),
                   derivation(EdgesNil, $),
                   derivation(Edge, edge.parameters(String.class, "src"), to.parameters(String.class, "dst"))),
            Graph
    );

}
