package dot;

import java.lang.String;
import java.util.function.Function;

interface Dot_ApiImpl {
  class Edges_Impl<K> implements Dot_Api.Edges<K> {
    private final Function<? super Dot_AstNode.Edges, K> k;

    Edges_Impl(final Function<? super Dot_AstNode.Edges, K> k) {
      this.k = k;
    }

    public Dot_Api.TO<Dot_Api.Edges<K>> edge(String src) {
      return new EdgesCons_Impl<>(k).edge(src);
    }

    public K $() {
      return new EdgesNil_Impl<>(k).$();
    }
  }

  class EdgesCons_Impl<K> extends Edge_Impl<Dot_Api.Edges<K>> implements Dot_Api.EdgesCons<K> {
    private final Function<? super Dot_AstNode.EdgesCons, K> k;

    EdgesCons_Impl(final Function<? super Dot_AstNode.EdgesCons, K> k) {
      super(x0 -> new Edges_Impl<>(x1 -> k.apply(new Dot_AstNode.EdgesCons(x0, x1))));
      this.k = k;
    }
  }

  class NodesNil_Impl<K> implements Dot_Api.NodesNil<K> {
    private final Function<? super Dot_AstNode.NodesNil, K> k;

    NodesNil_Impl(final Function<? super Dot_AstNode.NodesNil, K> k) {
      this.k = k;
    }

    public K $() {
      return k.apply(new Dot_AstNode.NodesNil());
    }
  }

  class Graph_Impl<K> extends Nodes_Impl<Dot_Api.Edges<K>> implements Dot_Api.Graph<K> {
    private final Function<? super Dot_AstNode.Graph, K> k;

    Graph_Impl(final Function<? super Dot_AstNode.Graph, K> k) {
      super(x0 -> new Edges_Impl<>(x1 -> k.apply(new Dot_AstNode.Graph(x0, x1))));
      this.k = k;
    }
  }

  class Edge_Impl<K> implements Dot_Api.Edge<K> {
    private final Function<? super Dot_AstNode.Edge, K> k;

    Edge_Impl(final Function<? super Dot_AstNode.Edge, K> k) {
      this.k = k;
    }

    public TO_Impl<K> edge(String src) {
      return new TO_Impl<>((dst) -> k.apply(new Dot_AstNode.Edge(src, dst)));
    }
  }

  class Node_Impl<K> implements Dot_Api.Node<K> {
    private final Function<? super Dot_AstNode.Node, K> k;

    Node_Impl(final Function<? super Dot_AstNode.Node, K> k) {
      this.k = k;
    }

    public K node(String name) {
      return k.apply(new Dot_AstNode.Node(name));
    }
  }

  class EdgesNil_Impl<K> implements Dot_Api.EdgesNil<K> {
    private final Function<? super Dot_AstNode.EdgesNil, K> k;

    EdgesNil_Impl(final Function<? super Dot_AstNode.EdgesNil, K> k) {
      this.k = k;
    }

    public K $() {
      return k.apply(new Dot_AstNode.EdgesNil());
    }
  }

  class Nodes_Impl<K> implements Dot_Api.Nodes<K> {
    private final Function<? super Dot_AstNode.Nodes, K> k;

    Nodes_Impl(final Function<? super Dot_AstNode.Nodes, K> k) {
      this.k = k;
    }

    public K $() {
      return new NodesNil_Impl<>(k).$();
    }

    public Dot_Api.Nodes<K> node(String name) {
      return new NodesCons_Impl<>(k).node(name);
    }
  }

  class NodesCons_Impl<K> extends Node_Impl<Dot_Api.Nodes<K>> implements Dot_Api.NodesCons<K> {
    private final Function<? super Dot_AstNode.NodesCons, K> k;

    NodesCons_Impl(final Function<? super Dot_AstNode.NodesCons, K> k) {
      super(x0 -> new Nodes_Impl<>(x1 -> k.apply(new Dot_AstNode.NodesCons(x0, x1))));
      this.k = k;
    }
  }

  class TO_Impl<K> implements Dot_Api.TO<K> {
    private final Function<String, K> k;

    TO_Impl(final Function<String, K> k) {
      this.k = k;
    }

    public K to(String dst) {
      return this.k.apply(dst);
    }
  }
}
