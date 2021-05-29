import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Aligner2 {
    Graph graph;
    List<Integer> topologicalOrder;


    public Aligner2(Graph graph, List<Integer> topologicalOrder) {
        this.graph = graph;
        this.topologicalOrder = topologicalOrder;
    }

    public int align(String pattern) {
        List<Integer> realEditDistances = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));
        List<Integer> iterationEditDistances = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));

        for (int j = 0; j < realEditDistances.size(); j++) {
            char char_in_graph = graph.graphSequence.get(j);
            realEditDistances.set(j,pattern.charAt(0) == char_in_graph ? 0 : 1);
            iterationEditDistances.set(j,pattern.charAt(0) == char_in_graph ? 0 : 1);
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

    public int alignCycle(String pattern) {
        List<Integer> realEditDistances = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));
        List<Integer> iterationEditDistances = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));

        for (int j = 0; j < realEditDistances.size(); j++) {
            char char_in_graph = graph.graphSequence.get(j);
            realEditDistances.set(j,pattern.charAt(0) == char_in_graph ? 0 : 1);
            iterationEditDistances.set(j,pattern.charAt(0) == char_in_graph ? 0 : 1);
        }

        for (int j = 1; j < pattern.length(); j++) {
            //for every node in graph
            for (int i = 0; i < graph.nodeIndexInGraphSequence.size(); i++) {
                this.f(realEditDistances, iterationEditDistances, pattern, j, i);
            }

            //Cv <- C'v
            List<Integer> tmp = realEditDistances;
            realEditDistances = iterationEditDistances;
            iterationEditDistances = tmp;


            for (int v = 0; v < graph.inNeighbors.size(); v++) {
                List<Integer> neighbors = graph.inNeighbors.get(v);
                for (int u = 0; u < neighbors.size(); u++) {
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

    private void g(List<Integer> realEditDistances, List<Integer> iterationEditDistances, String pattern, int j, Integer node) {
        int start = graph.getNodeStartInSequence(node);
        int end = graph.getNodeEndInSequence(node);
        iterationEditDistances.set(start, j - 1);
        char charInGraph = graph.graphSequence.get(start);
        boolean match = charInGraph == pattern.charAt(j);

        int smallestNeighbor = Integer.MAX_VALUE;
        for (int neighbor : graph.inNeighbors.get(node)) {
            int lastInNeighborIndex = graph.getNodeEndInSequence(neighbor) - 1;
            smallestNeighbor = Math.min(smallestNeighbor, realEditDistances.get(lastInNeighborIndex));
        }

        if (match) {
            iterationEditDistances.set(start, Math.min(j - 1, smallestNeighbor));
        } else {
            iterationEditDistances.set(start, 1 + Math.min(realEditDistances.get(start), smallestNeighbor));
        }

        for (int c = start + 1; c < end; c++) {
            charInGraph = graph.graphSequence.get(c);
            match = charInGraph == pattern.charAt(j);
            if (match) {
                iterationEditDistances.set(c, Math.min(j - 1, realEditDistances.get(c - 1)));
            } else {
                iterationEditDistances.set(c, 1 + Math.min(realEditDistances.get(c), realEditDistances.get(c-1)));
            }
        }
    }

    private void f(List<Integer> realEditDistances, List<Integer> iterationEditDistances, String pattern, int j, Integer node) {
        int start = graph.getNodeStartInSequence(node);
        int end = graph.getNodeEndInSequence(node);
        //iterationEditDistances.set(start, j - 1);

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

/*
        for (int neighbor : graph.inNeighbors.get(node)) {
            int lastInNeighborIndex = graph.getNodeEndInSequence(neighbor) - 1;
            current_slice.set(start, Math.min(current_slice.get(start),
                    Math.min(current_slice.get(lastInNeighborIndex) + 1,
                            previous_slice.get(lastInNeighborIndex) + (same ? 0 : 1))));
        }

 */


        if (match) {
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
                iterationEditDistances.set(c, 1 + Math.min(realEditDistances.get(c), Math.min(realEditDistances.get(c-1), iterationEditDistances.get(c-1))));
            }
        }


    }

    public void propagate(int u, int v, List<Integer> realEditDistances) {
        int u_end = graph.getNodeEndInSequence(u) - 1;
        int v_start = graph.nodeIndexInGraphSequence.get(v);
        int v_end = graph.getNodeEndInSequence(v) - 1;
        int new_value = realEditDistances.get(u_end) + 1;
        if (realEditDistances.get(v_start) > new_value) {
            realEditDistances.set(v_start, new_value);
            //propagate further inside node
            new_value++;
            for (int c = v_start+1; c < v_end; c++) {
                if (realEditDistances.get(c) > new_value) {
                    realEditDistances.set(c, new_value);
                    new_value++;
                }
                else
                    break;
            }
            for (int z : graph.outNeighbors.get(v)) {
                propagate(v, z, realEditDistances);
            }
        }
    }

    /*
    public int alignCyclic(String pattern) {
        List<Integer> currentSlice = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));
        List<Integer> previousSlice = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));


        for (int w = 0; w < previousSlice.size(); w++) {
            char char_in_graph = graph.graphSequence.get(w);
            previousSlice.set(w, sequence.charAt(0) == char_in_graph ? 0 : 1);
            //previousSlice.set(w, sequence.charAt(0) == char_in_graph ? 0 : 1);
        }


        for (int j = 1; j < pattern.length(); j++) {
            for (int i = 0; i < currentSlice.size(); i++)
                currentSlice.set(i, pattern.length());

            for (var component : componentOrder) {
                if (component.size() == 1) {
                    boolean isAcyclic = true;
                    for (int neighbor : graph.inNeighbors.get(component.get(0))) {
                        if (neighbor == component.get(0)) {
                            isAcyclic = false;
                            break;
                        }
                    }
                    if (isAcyclic) {
                        //calculateAcyclic(currentSlice, previousSlice, pattern, j, component.get(0));
                        continue;
                    }
                }
                this.calculateCyclic(pattern, component, currentSlice, previousSlice, j);
            }
            List<Integer> tmp = currentSlice;
            currentSlice = previousSlice;
            previousSlice = tmp;
        }
        int smallest = Integer.MAX_VALUE;

        for (int i = 0; i < previousSlice.size(); i++) {
            smallest = Math.min(smallest, previousSlice.get(i));
        }
        return smallest;
    }

    private void calculateCyclic(String pattern, List<Integer> nodes, List<Integer> currentSlice, List<Integer> previousSlice, int j) {
        for (int node : nodes) {
            //calculateAcyclic(currentSlice, previousSlice, pattern, j, node);
        }

        for (int node : nodes) {
            int start = graph.getNodeStartInSequence(node);

            for (int neighbor : graph.inNeighbors.get(node)) {
                int lastInNeighborIndex = graph.getNodeEndInSequence(neighbor) - 1;
                if (currentSlice.get(start) > currentSlice.get(lastInNeighborIndex) + 1) {
                    this.recurseHorizontalScores(currentSlice, node, start, currentSlice.get(lastInNeighborIndex) + 1);
                }
            }

            for (int c = start + 1; c < graph.getNodeEndInSequence(node); c++) {
                if (currentSlice.get(c) > currentSlice.get(c - 1) + 1) {
                    this.recurseHorizontalScores(currentSlice, node, c, currentSlice.get(c - 1) + 1);
                }
            }
        }
    }

    private void recurseHorizontalScores(List<Integer> currentSlice, int node, int start, int newScore) {
        currentSlice.set(start, newScore);
        newScore++;
        for (int c = start + 1; c < graph.getNodeEndInSequence(node); c++) {
            if (currentSlice.get(c) <= newScore)
                return;
            currentSlice.set(c, newScore);
            newScore++;
        }
        for (int neighbor : graph.outNeighbors.get(node)) {
            if (belongsToComponent.get(neighbor).equals(belongsToComponent.get(node))) {
                int neighborStartIndex = graph.getNodeStartInSequence(neighbor);
                if (currentSlice.get(neighborStartIndex) > newScore)
                    recurseHorizontalScores(currentSlice, neighbor, neighborStartIndex, newScore);
            }
        }
    }

     */

}


