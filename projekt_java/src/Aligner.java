import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Aligner {

    Graph graph;
    List<Integer> topologicalOrder;

    public Aligner(Graph graph, List<Integer> topologicalOrder) {
        this.graph = graph;
        this.topologicalOrder = topologicalOrder;
    }

    /**
     * Aligns acyclic graph with pattern and returns smallest edit distance.
     * @param pattern
     * @return smallest edit distance
     */
    public int align(String pattern) {
        List<Integer> realEditDistances = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));
        List<Integer> iterationEditDistances = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));

        for (int j = 0; j < realEditDistances.size(); j++) {
            char char_in_graph = graph.graphSequence.get(j);
            realEditDistances.set(j, pattern.charAt(0) == char_in_graph ? 0 : 1);
            iterationEditDistances.set(j, pattern.charAt(0) == char_in_graph ? 0 : 1);
        }

        for (int j = 1; j < pattern.length(); j++) {
            //for every node in graph
            for (int node : topologicalOrder) {
                this.f(realEditDistances, iterationEditDistances, pattern, j, node);
            }

            //Cv <- C'v
            List<Integer> tmp = realEditDistances;
            realEditDistances = iterationEditDistances;
            iterationEditDistances = tmp;
        }

        int smallest = Integer.MAX_VALUE;
        for (Integer realEditDistance : realEditDistances) {
            smallest = Math.min(smallest, realEditDistance);
        }
        return smallest;
    }


    /**
     * Aligns cyclic graph with pattern and returns smallest edit distance.
     * @param pattern
     * @return smallest edit distance
     */
    public int alignCycle(String pattern) {
        List<Integer> realEditDistances = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));
        List<Integer> iterationEditDistances = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));

        for (int j = 0; j < realEditDistances.size(); j++) {
            char char_in_graph = graph.graphSequence.get(j);
            realEditDistances.set(j, pattern.charAt(0) == char_in_graph ? 0 : 1);
            iterationEditDistances.set(j, pattern.charAt(0) == char_in_graph ? 0 : 1);
        }

        for (int j = 1; j < pattern.length(); j++) {
            for (int i = 0; i < iterationEditDistances.size(); i++)
                iterationEditDistances.set(i, pattern.length());
            //for every node in graph
            for (int node : topologicalOrder) {
                this.f(realEditDistances, iterationEditDistances, pattern, j, node);
            }

            //Cv <- C'v
            List<Integer> tmp = realEditDistances;
            realEditDistances = iterationEditDistances;
            iterationEditDistances = tmp;

            for (int v = 0; v < graph.getNumberOfNodesInGraph(); v++) {
                for (int u : graph.inNeighbors.get(v)) {
                    propagate(u, v, realEditDistances);
                }
            }
        }

        int smallest = Integer.MAX_VALUE;
        for (Integer realEditDistance : realEditDistances) {
            smallest = Math.min(smallest, realEditDistance);
        }
        return smallest;
    }


    /**
     * f function from Navarro algorithm.
     * @param realEditDistances
     * @param iterationEditDistances
     * @param pattern
     * @param j
     * @param node
     */
    private void f(List<Integer> realEditDistances, List<Integer> iterationEditDistances, String pattern, int j, Integer node) {
        int start = graph.getNodeStartInSequence(node);
        int end = graph.getNodeEndInSequence(node);

        //instead of j-1 we take last iteration + 1
        iterationEditDistances.set(start, realEditDistances.get(start) + 1);

        char charInGraph = graph.graphSequence.get(start);
        boolean match = charInGraph == pattern.charAt(j);

        int smallestNeighbor = Integer.MAX_VALUE;
        for (int neighbor : graph.inNeighbors.get(node)) {
            int lastInNeighborIndex = graph.getNodeEndInSequence(neighbor) - 1;
            smallestNeighbor = Math.min(smallestNeighbor, realEditDistances.get(lastInNeighborIndex));
        }

        int smallestIterationNeighbor = Integer.MAX_VALUE;
        for (int neighbor : graph.inNeighbors.get(node)) {
            int lastInNeighborIndex = graph.getNodeEndInSequence(neighbor) - 1;
            smallestIterationNeighbor = Math.min(smallestIterationNeighbor, iterationEditDistances.get(lastInNeighborIndex));
        }

        if (match) {
            //instead of j-1 we take last iteration + 1
            iterationEditDistances.set(start, Math.min(iterationEditDistances.get(start), smallestNeighbor));
        } else {
            iterationEditDistances.set(start, 1 + Math.min(realEditDistances.get(start), Math.min(smallestNeighbor, smallestIterationNeighbor)));
        }

        for (int c = start + 1; c < end; c++) {
            charInGraph = graph.graphSequence.get(c);
            match = charInGraph == pattern.charAt(j);
            if (match) {
                iterationEditDistances.set(c, realEditDistances.get(c - 1));
                //iterationEditDistances.set(c, Math.min(j - 1, realEditDistances.get(c - 1)));
            } else {
                iterationEditDistances.set(c, 1 + Math.min(realEditDistances.get(c), Math.min(realEditDistances.get(c - 1), iterationEditDistances.get(c - 1))));
            }
        }
    }

    /**
     * Propagation function from Navarro algorithm.
     * @param u
     * @param v
     * @param realEditDistances
     */
    public void propagate(int u, int v, List<Integer> realEditDistances) {
        int u_end = graph.getNodeEndInSequence(u) - 1;
        int v_start = graph.nodeIndexInGraphSequence.get(v);
        int v_end = graph.getNodeEndInSequence(v) - 1;
        int new_value = realEditDistances.get(u_end) + 1;
        if (realEditDistances.get(v_start) > new_value) {
            realEditDistances.set(v_start, new_value);
            //propagate further inside node
            new_value++;
            for (int c = v_start + 1; c < v_end; c++) {
                if (realEditDistances.get(c) > new_value) {
                    realEditDistances.set(c, new_value);
                    new_value++;
                } else
                    break;
            }
            for (int z : graph.outNeighbors.get(v)) {
                propagate(v, z, realEditDistances);
            }
        }
    }
}