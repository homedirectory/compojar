package compojar.scratch;

import dot.Dot_AstNode.*;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static compojar.util.T2.t2;
import static dot.Dot_Api.start;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;

public class DotTest {

    @Test
    public void a() {
        var graph = start()
                .node("A")
                .node("B")
                .node("C")
                .$()
                .edge("A").to("B")
                .edge("C").to("A")
                .$();

        assertEquals(Set.of("A", "B", "C"), getNodes(graph).stream().map(node -> node.name).collect(toSet()));
        assertEquals(Set.of(t2("A", "B"), t2("C", "A")),
                     getEdges(graph).stream().map(e -> t2(e.src, e.dst)).collect(toSet()));
    }

    private static Set<Node> getNodes(Graph graph) {
        var nodes = new HashSet<Node>();

        var curr = graph.nodes;
        while (curr instanceof NodesCons cons) {
            nodes.add(cons.node);
            curr = cons.nodes;
        }

        return unmodifiableSet(nodes);
    }

    private static Set<Edge> getEdges(Graph graph) {
        var edges = new HashSet<Edge>();

        var curr = graph.edges;
        while (curr instanceof EdgesCons cons) {
            edges.add(cons.edge);
            curr = cons.edges;
        }

        return unmodifiableSet(edges);
    }

}
