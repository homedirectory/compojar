package compojar.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static compojar.model.Keys.*;
import static compojar.util.Util.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.iterate;

/**
 * Encoding of grammar trees in the DOT language for the GraphViz tool.
 */
public final class GraphViz {

    public static String toDot(final GrammarTreeModel model) {
        Map<GrammarTree, String> dot_nodeNames = zip(model.nodes().stream(), iterate(1, i -> i + 1))
                .collect(toMapFromPairs((node, $) -> node,
                                        ($, i) -> "x" + i));

        List<String> dot_nodeDefs = stream(dot_nodeNames,
                                           (node, dot_name) -> "%s [label=\"%s\"]".formatted(dot_name, node.name()))
                .toList();

        List<String> dot_edgeDefs = model
                .nodes()
                .stream()
                .flatMap(node -> {
                    var children = model.get(node, CHILDREN).orElseGet(Set::of);
                    if (children.isEmpty()) {
                        return Stream.of();
                    }
                    else {
                        var result = switch (node) {
                            case GrammarTree.Node it -> "%s -> %s"
                                    .formatted(dot_nodeNames.get(it),
                                               dot_nodeNames.get(first(children)));
                            case GrammarTree.FreeNode it -> "%s -> {%s} [style=dashed]"
                                    .formatted(dot_nodeNames.get(it),
                                               children.stream().map(dot_nodeNames::get).collect(joining(" ")));
                            case GrammarTree.Leaf it -> throw new IllegalStateException(format("Unexpected node with children: %s", it));
                        };
                        return Stream.of(result);
                    }
                })
                .toList();

        List<List<GrammarTree>> nextsGroups = model
                .nodes()
                .stream()
                .filter(node -> model.has(node, PARENT) && model.has(node, NEXT))
                .map(node -> Stream.concat(Stream.of(node), allNexts(model, node)).toList())
                .toList();
        List<String> dot_nextEdgeDefs = nextsGroups
                .stream()
                .map(nexts -> nexts.stream().map(dot_nodeNames::get).collect(joining(" -> ", "", " [color=blue]")))
                .toList();

        List<String> dot_ranks = nextsGroups
                .stream()
                .map(nexts -> "{ rank = same; %s }".formatted(nexts.stream().map(dot_nodeNames::get).collect(joining(" "))))
                .toList();

        return """
        digraph tree {
            node [style=filled fillcolor=white]

            %s

            %s

            %s
        }
        """.formatted(String.join(";\n", dot_nodeDefs),
                      Stream.concat(dot_edgeDefs.stream(), dot_nextEdgeDefs.stream()).collect(joining(";\n")),
                      String.join(";\n", dot_ranks));
    }

    public static void toDotFile(GrammarTreeModel model, Path path) {
        try {
            Files.writeString(path, toDot(model), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void toDotFile(GrammarTreeModel model, CharSequence path) {
        toDotFile(model, Path.of(path.toString()));
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private GraphViz() {}

}
