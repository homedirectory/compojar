package dot;

import java.lang.String;

public interface Dot_AstNode {
  class EdgesCons implements Edges, Dot_AstNode {
    public final Edge edge;

    public final Edges edges;

    public EdgesCons(final Edge edge, final Edges edges) {
      this.edge = edge;
      this.edges = edges;
    }
  }

  class NodesCons implements Nodes, Dot_AstNode {
    public final Node node;

    public final Nodes nodes;

    public NodesCons(final Node node, final Nodes nodes) {
      this.node = node;
      this.nodes = nodes;
    }
  }

  class Graph implements Dot_AstNode {
    public final Nodes nodes;

    public final Edges edges;

    public Graph(final Nodes nodes, final Edges edges) {
      this.nodes = nodes;
      this.edges = edges;
    }
  }

  interface Nodes extends Dot_AstNode {
  }

  class Node implements Dot_AstNode {
    public final String name;

    public Node(final String name) {
      this.name = name;
    }
  }

  class Edge implements Dot_AstNode {
    public final String src;

    public final String dst;

    public Edge(final String src, final String dst) {
      this.src = src;
      this.dst = dst;
    }
  }

  class EdgesNil implements Edges, Dot_AstNode {
    public EdgesNil() {
    }
  }

  interface Edges extends Dot_AstNode {
  }

  class NodesNil implements Nodes, Dot_AstNode {
    public NodesNil() {
    }
  }
}
