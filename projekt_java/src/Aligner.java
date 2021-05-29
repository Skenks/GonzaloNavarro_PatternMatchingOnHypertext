import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Aligner {
    Graph graph;
    List<List<Integer>> componentOrder;
    List<Integer> belongsToComponent;

    public Aligner(Graph graph, List<List<Integer>> componentOrder, List<Integer> belongsToComponent) {
        this.graph = graph;
        this.componentOrder = componentOrder;
        this.belongsToComponent = belongsToComponent;
    }

    public int align(String pattern) {
        List<Integer> current_slice = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));
        List<Integer> previous_slice = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));


        for (int w = 0; w < previous_slice.size(); w++) {
            char char_in_graph = graph.graphSequence.get(w);
            previous_slice.set(w, pattern.charAt(0) == char_in_graph ? 0 : 1);
        }

        for (int j = 1; j < pattern.length(); j++) {
            for (int i = 0; i < graph.nodeIndexInGraphSequence.size(); i++) {
                this.calculateAcyclic(current_slice, previous_slice, pattern, j, i);
            }
            List<Integer> tmp = current_slice;
            current_slice = previous_slice;
            previous_slice = tmp;
        }

        int smallest = Integer.MAX_VALUE;

        for (int i = 0; i < previous_slice.size(); i++) {
            smallest = Math.min(smallest, previous_slice.get(i));
        }
        return smallest;
    }

    private void calculateAcyclic(List<Integer> current_slice, List<Integer> previous_slice, String pattern, int j, Integer node) {
        int start = graph.getNodeStartInSequence(node);
        int end = graph.getNodeEndInSequence(node);
        current_slice.set(start, previous_slice.
                get(start) + 1);
        char charInGraph = graph.graphSequence.get(start);
        boolean same = charInGraph == pattern.charAt(j);
        for (int neighbor : graph.inNeighbors.get(node)) {
            int lastInNeighborIndex = graph.getNodeEndInSequence(neighbor) - 1;
            current_slice.set(start, Math.min(current_slice.get(start),
                    Math.min(current_slice.get(lastInNeighborIndex) + 1,
                            previous_slice.get(lastInNeighborIndex) + (same ? 0 : 1))));
        }
        for (int c = start + 1; c < end; c++) {
            charInGraph = graph.graphSequence.get(c);
            same = charInGraph == pattern.charAt(j);
            current_slice.set(c, Math.min(current_slice.get(c - 1) + 1,
                    Math.min(previous_slice.get(c) + 1, previous_slice.get(c - 1) + (same ? 0 : 1))));
        }
    }

    public int alignCyclic(String pattern) {
        List<Integer> currentSlice = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));
        List<Integer> previousSlice = new ArrayList<>(Collections.nCopies(graph.graphSequence.size(), 0));

        /*
        for (int w = 0; w < previousSlice.size(); w++) {
            char char_in_graph = graph.graphSequence.get(w);
            previousSlice.set(w, sequence.charAt(0) == char_in_graph ? 0 : 1);
            //previousSlice.set(w, sequence.charAt(0) == char_in_graph ? 0 : 1);
        }
         */

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
                        calculateAcyclic(currentSlice, previousSlice, pattern, j, component.get(0));
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
            calculateAcyclic(currentSlice, previousSlice, pattern, j, node);
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
}


