package compojar.dot;

import java.util.function.Function;

public interface Dot_Api {
  static Graph<Dot_AstNode.Graph> start() {
    return new Dot_ApiImpl.Graph_Impl<>(Function.identity());
  }

  interface Node<K> {
    K node(String name);
  }

  interface Edges<K> extends EdgesCons<K>, EdgesNil<K> {
  }

  interface Nodes<K> extends NodesNil<K>, NodesCons<K> {
  }

  interface NodesCons<K> extends Node<Nodes<K>> {
  }

  interface TO<K> {
    K to(String dst);
  }

  interface EdgesNil<K> {
    K $();
  }

  interface EdgesCons<K> extends Edge<Edges<K>> {
  }

  interface NodesNil<K> {
    K $();
  }

  interface Graph<K> extends Nodes<Edges<K>> {
  }

  interface Edge<K> {
    TO<K> edge(String src);
  }
}
