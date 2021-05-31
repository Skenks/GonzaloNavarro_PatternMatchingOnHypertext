import java.util.*;

/**
 * Graph that stores nodes and edges.
 */
public class Graph {

    /**
     * Represents overlap from .gfa file which is important when we are creating nodes.
     * If overlap is 10, then we are removing 10 last characters from node sequence.
     */
    int overlap;

    /**
     * sequence that stores chars of all nodes in graph
     */
    List<Character> graphSequence = new ArrayList<>();

    /**
     * map that normalizes nodes - if nodes start from 0 or 1 or other we will be able to save them
     * in nodeIndexInGraphSequence from first index - 0
     */
    Map<Integer, Integer> nodesMap = new HashMap<>();

    /**
     * stores index of every node start in graph sequence.
     * Example: if second node from graph file starts at index 100 in graphSequence then
     * nodeIndexInGraphSequence.get(1) returns 100
     */
    List<Integer> nodeIndexInGraphSequence = new ArrayList<>();

    /**
     * For every node(n1) in graph stores stores list of nodes(nx, ...) for whom there is edge e that goes from nx to n1
     */
    List<List<Integer>> inNeighbors = new ArrayList<>();

    /**
     * For every node(n1) in graph stores stores list of nodes(n2, ...) for whom there is edge e that goes from n1 to nx
     */
    List<List<Integer>> outNeighbors = new ArrayList<>();

    /**
     * Returns index of node start in graph sequence.
     * @param node
     * @return
     */
    public int getNodeStartInSequence(int node) {
        return nodeIndexInGraphSequence.get(node);
    }

    /**
     * Returns index of (node end + 1) in graph sequence.
     * @param node
     * @return
     */
    public int getNodeEndInSequence(int node) {
        if (node == this.nodeIndexInGraphSequence.size() - 1)
            return this.graphSequence.size();
        return nodeIndexInGraphSequence.get(node + 1);
    }

    /**
     * Returns number of nodes in graph.
     * @return
     */
    public int getNumberOfNodesInGraph() {
        return nodeIndexInGraphSequence.size();
    }


    /**
     * Adds node in graph (adds sequence of node in graphSequence and adds start of node in list of node starts)
     * and does necessary work (reserve space for neighbors)
     * @param node
     */
    public void addNode(Node node) {
        //some nodes start from 0, some from 1 - map normalizes it
        nodesMap.put(node.id, getNumberOfNodesInGraph());
        nodeIndexInGraphSequence.add(graphSequence.size());
        inNeighbors.add(new ArrayList<>());
        outNeighbors.add(new ArrayList<>());
        for (char c : node.sequence.toCharArray()) {
            graphSequence.add(c);
        }
    }


    /**
     * Creates edges for normal and reverse-complement graph. Adds edges to inNeighbors and outNeighbors.
     * @param line
     */
    public void createAndAddEdges(String line) {
        String[] arr = line.split("\t");
        int from_node_original_id = Integer.parseInt(arr[1]);
        String from_sign = arr[2]; //+ ili -
        int to_node_original_id = Integer.parseInt(arr[3]);
        String to_sign = arr[4];
        int from_normal, from_reverse, to_normal, to_reverse;

        if (from_sign.equals("+")) {
            from_normal = from_node_original_id * 2;
            from_reverse = from_node_original_id * 2 + 1;
        } else {
            from_normal = from_node_original_id * 2 + 1;
            from_reverse = from_node_original_id * 2;
        }
        if (to_sign.equals("+")) {
            to_normal = to_node_original_id * 2;
            to_reverse = to_node_original_id * 2 + 1;
        } else {
            to_normal = to_node_original_id * 2 + 1;
            to_reverse = to_node_original_id * 2;
        }

        int from_node = nodesMap.get(from_normal);
        int to_node = nodesMap.get(to_normal);


        if (!this.inNeighbors.get(to_node).contains(from_node)) {
            this.inNeighbors.get(to_node).add(from_node);
        }
        if (!this.outNeighbors.get(from_node).contains(to_node)) {
            this.outNeighbors.get(from_node).add(to_node);
        }


        from_node = nodesMap.get(to_reverse);
        to_node = nodesMap.get(from_reverse);


        if (!this.inNeighbors.get(to_node).contains(from_node)) {
            this.inNeighbors.get(to_node).add(from_node);
        }
        if (!this.outNeighbors.get(from_node).contains(to_node)) {
            this.outNeighbors.get(from_node).add(to_node);
        }
    }


    /**
     * Kahn’s algorithm for Topological Sorting (1962.)
     * @return topological order of nodes in graph if the graph is acyclic else throws error
     * @throws GraphCycleException
     */
    public List<Integer> topologicalOrderOfNodesAcyclic() throws GraphCycleException {
        //Empty list that will contain the sorted elements
        List<Integer> L = new ArrayList<>();
        // Set of all nodes with no incoming edge
        List<Integer> S = new ArrayList<>();
        List<List<Integer>> inNeighborsCopy = Utility.copyNeighbors(inNeighbors);
        List<List<Integer>> outNeighborsCopy = Utility.copyNeighbors(outNeighbors);
        //find nodes with no incoming edge
        for (int i = 0; i < inNeighbors.size(); i++) {
            if (inNeighbors.get(i).size() == 0)
                S.add(i);
        }
        while (!S.isEmpty()) {
            int n = S.get(0);
            S.remove(0);
            L.add(n);
            for (int m : outNeighbors.get(n)) {
                outNeighborsCopy.get(n).remove(Integer.valueOf(m));
                inNeighborsCopy.get(m).remove(Integer.valueOf(n));
                if (inNeighborsCopy.get(m).isEmpty())
                    S.add(m);
            }
        }

        for (List<Integer> neighbors : inNeighborsCopy) {
            if (!neighbors.isEmpty()) {
                //(graph has at least one cycle)
                throw new GraphCycleException();
            }
        }
        //(a topologically sorted order)
        return L;
    }

    /**
     * https://www.geeksforgeeks.org/topological-sorting/
     *
     * Time Complexity: O(V+E).
     * The above algorithm is simply DFS with an extra stack. So time complexity is the same as DFS.
     * Auxiliary space: O(V).
     * The extra space is needed for the stack.
     * @return topological order of nodes in graph
     */
    public List<Integer> topologicalOrderOfNodesCyclic() {
        List<Integer> stack;
        // Mark all the vertices as not visited
        List<Boolean> visited;
        stack = new ArrayList<>();
        // Mark all the vertices as not visited
        visited = new ArrayList<>(Collections.nCopies(getNumberOfNodesInGraph(), false));

        for (int i = 0; i < getNumberOfNodesInGraph(); i++) {
            if (!visited.get(i))
                topologicalSortUtil(i, stack, visited);
        }

        Collections.reverse(stack);
        return stack;
    }

    /**
     * Util function for topologicalOrderOfNodesCyclic
     * @param v
     * @param stack
     * @param visited
     */
    public void topologicalSortUtil(int v, List<Integer> stack, List<Boolean> visited) {
        visited.set(v, true);
        List<Integer> neighbors = outNeighbors.get(v);
        for (int i = 0; i < neighbors.size(); i++) {
            if (!visited.get(neighbors.get(i)))
                topologicalSortUtil(neighbors.get(i), stack, visited);
        }
        stack.add(v);
    }
}
