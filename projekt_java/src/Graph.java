import java.util.*;

import static java.util.logging.Logger.global;

public class Graph {

    private final static int MAX_NODE_SIZE = 100;

    //nodeLookup
    private Map<Integer, List<Integer>> splitNodesMap = new HashMap<Integer, List<Integer>>();
    private int overlap;
    List<Character> graphSequence = new ArrayList<>();
    //koristimo isti node size kako bi se
    //nodeStart
    List<Integer> nodeIndexInGraphSequence = new ArrayList<>();
    List<List<Integer>> inNeighbors = new ArrayList<>();
    List<List<Integer>> outNeighbors = new ArrayList<>();
    List<Boolean> reverse = new ArrayList<>();
    int indexnum;

    public int getNodeStartInSequence(int node) {
        return nodeIndexInGraphSequence.get(node);
    }

    public int getNodeEndInSequence(int node) {
        if (node == this.nodeIndexInGraphSequence.size()-1)
            return this.graphSequence.size();
        return nodeIndexInGraphSequence.get(node+1);
    }

    public int getOverlap() {
        return overlap;
    }

    public void setOverlap(int overlap) {
        this.overlap = overlap;
    }

    /**
     * Cijepa node ako je potrebno na manje dijelove (npr. za linear graf) i dodaje ih u graf.
     *
     * @param node
     */
    public void addNode(Node node) {
        int i = 0;
        while (i < node.sequence.length()) {
            char[] sequenceToAdd;
            try {
                sequenceToAdd = node.sequence.substring(i, i + MAX_NODE_SIZE).toCharArray();
            } catch (StringIndexOutOfBoundsException e) {
                sequenceToAdd = node.sequence.substring(i).toCharArray();
            }
            if (!splitNodesMap.containsKey(node.getId()))
                splitNodesMap.put(node.getId(), new ArrayList<>());
            splitNodesMap.get(node.getId()).add(nodeIndexInGraphSequence.size());
            nodeIndexInGraphSequence.add(graphSequence.size());
            inNeighbors.add(new ArrayList<>());
            outNeighbors.add(new ArrayList<>());
            reverse.add(node.isReversed());

            for (char c : sequenceToAdd) {
                graphSequence.add(c);
            }

            if (i > 0) { //if the node is split
                //add neighbor edges between split nodes - edge going out of second last into last
                outNeighbors.get(outNeighbors.size() - 2).add(outNeighbors.size() - 1);
                inNeighbors.get(inNeighbors.size() - 1).add(inNeighbors.size() - 2);
            }
            i += MAX_NODE_SIZE;
        }
    }

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

        //dodaj prvi čvor - za normalni graf - (from_normal, to_normal)
        //zadnji splitani cvor s tim id-om
        int from_node = this.splitNodesMap.get(from_normal).get(splitNodesMap.get(from_normal).size() - 1);
        //prvi splitani cvor s tim id-om
        int to_node = this.splitNodesMap.get(to_normal).get(0);

        //dodijeljivanje susjeda cvorovima
        if (!this.inNeighbors.get(to_node).contains(from_node)) {
            this.inNeighbors.get(to_node).add(from_node);
        }
        if (!this.outNeighbors.get(from_node).contains(to_node)) {
            this.outNeighbors.get(from_node).add(to_node);
        }

        //dodaj drugi čvor - za obrnuti komplementarni graf - (to_reverse, from_reverse)
        //zadnji splitani cvor s tim id-om
        from_node = this.splitNodesMap.get(to_reverse).get(splitNodesMap.get(to_reverse).size() - 1);
        //prvi splitani cvor s tim id-om
        to_node = this.splitNodesMap.get(from_reverse).get(0);

        //dodijeljivanje susjeda cvorovima
        if (!this.inNeighbors.get(to_node).contains(from_node)) {
            this.inNeighbors.get(to_node).add(from_node);
        }
        if (!this.outNeighbors.get(from_node).contains(to_node)) {
            this.outNeighbors.get(from_node).add(to_node);
        }
    }

    public void summary() {
        System.out.println(splitNodesMap.size() + " original nodes");
        System.out.println(graphSequence.size() + " length of graph sequence");
        System.out.println(nodeIndexInGraphSequence.size() + "split nodes");
        int special_nodes = 0, edges = 0;
        for (int i = 0; i < inNeighbors.size(); i++) {
            if (inNeighbors.get(i).size() >= 2) {
                special_nodes += 1;
            }
            edges += inNeighbors.get(i).size();
        }
        System.out.println(edges+" edges");
        System.out.println(special_nodes+" nodes with in-degree >= 2");
    }

    public List<List<Integer>> topologicalOrderOfComponents() {
        List<Integer> index = new ArrayList<>(Collections.nCopies(nodeIndexInGraphSequence.size(), -1));
        List<Integer> lowlink = new ArrayList<>(Collections.nCopies(nodeIndexInGraphSequence.size(), -1));
        List<Boolean> onStack = new ArrayList<>(Collections.nCopies(nodeIndexInGraphSequence.size(), false));
        Stack<Integer> S = new Stack<>();
        indexnum = 0;
        List<List<Integer>> result = new ArrayList<>();
        for(int i = 0; i < nodeIndexInGraphSequence.size(); i++) {
            if (index.get(i) == -1)
                connect(i, result, index, lowlink, onStack, S);
        }
        Collections.reverse(result);

        assert(result.size() > 0);
        assert(result.get(0).size() > 0);
        assert(result.get(result.size()-1).size() > 0);
        return result;
    }

    public void connect(int node, List<List<Integer>> result, List<Integer> index,
                        List<Integer> lowlink, List<Boolean> onStack, Stack<Integer> S) {
        index.set(node, indexnum);
        lowlink.set(node, indexnum);
        indexnum++;
        S.add(node);
        onStack.set(node, true);
        System.out.println("node="+node);
        for (int neighbor : outNeighbors.get(node)) {
            if (index.get(neighbor) == -1) {
                connect(neighbor, result, index, lowlink, onStack, S);
                lowlink.set(node, Math.min(lowlink.get(neighbor), lowlink.get(node)));
            } else if (onStack.get(neighbor)) {
                lowlink.set(node, Math.min(lowlink.get(node), index.get(neighbor)));
            }
        }
        if(lowlink.get(node).equals(index.get(node))) {
            result.add(new ArrayList<>());
            int i = 0;
            while(true) {
                int w = S.pop();
                onStack.set(w, false);
                result.get(result.size()-1).add(w);
                if (w == node)
                    break;
                else
                    i++;
            }
        }


    }
}