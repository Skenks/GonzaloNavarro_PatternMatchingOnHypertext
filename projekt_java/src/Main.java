import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        String graphName = args[0];
        String fastqName = args[1];
        String scoresFile = graphName.substring(0, graphName.length()-3) + "txt";
        Path bit_parallel_file = Path.of("bit_parallel_scores/" + scoresFile);
        Graph graph = Utility.readGraph(Path.of("graphs/"+graphName));
        List<FastQ> fastqs = Utility.readFastq(Path.of("graphs/"+fastqName));
        List<Integer> bit_parallel_scores = Utility.loadScores(bit_parallel_file);

        List<Integer> topologicalOrder = graph.topologicalOrderOfNodes();


        List<List<Integer>>componentOrder = new ArrayList<>();
        List<Integer> belongsToComponent = new ArrayList<>(Collections.nCopies(graph.nodeIndexInGraphSequence.size(), -1));
        for (int component = 0; component < componentOrder.size(); component++) {
            assert componentOrder.get(component).size() > 0;
            for (int node : componentOrder.get(component)) {
                belongsToComponent.set(node, component);
            }
        }

        for (int i = 0; i < graph.nodeIndexInGraphSequence.size(); i++) {
            assert(belongsToComponent.get(i) != -1);
            for (int neighbor : graph.inNeighbors.get(i)) {
                assert(belongsToComponent.get(neighbor) <= belongsToComponent.get(i));
            }
        }



        Aligner2 aligner2 = new Aligner2(graph, topologicalOrder);
        Aligner aligner = new Aligner(graph, componentOrder, belongsToComponent);


        boolean isAcyclic = true;
        boolean isTree = true;

        for (int i = 0; i < componentOrder.size(); i++)
        {
            if (componentOrder.get(i).size() > 1)
            {
                isAcyclic = false;
                break;
            }
            for (int neighbor : graph.inNeighbors.get(componentOrder.get(i).get(0))) { //susjedi od prvog u komponenti
                if (neighbor == componentOrder.get(i).get(0)) //ako je neki od susjeda isti
                {
                    isAcyclic = false;
                    break;
                }
            }
        }
        if (isAcyclic)
        {
            for (int i = 0; i < graph.nodeIndexInGraphSequence.size(); i++)
            {
                if (graph.inNeighbors.get(i).size() > 1)
                {
                    isTree = false;
                    break;
                }
            }
        }
        else
        {
            isTree = false;
        }

        if (isTree)
        {
            assert(isAcyclic);
            System.out.println("The graph is linear / a tree / a forest");
        }
        else if (isAcyclic)
        {
            System.out.println("The graph is DAG");
        }
        else
        {
            System.out.println("The graph is cyclic");
        }



        List<Integer> scores = new ArrayList<>();
        for (FastQ fastq : fastqs) {
            int score = 0;
            if (isAcyclic) {
                score = aligner2.align(fastq.sequence);
            } else
                //score = aligner.alignCyclic(fastq.sequence);
                score = 1;
            System.out.println(score);
            scores.add(score);
        }
        int correct = 0;
        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i).equals(bit_parallel_scores.get(i)))
                correct++;
        }
        System.out.println(correct+"/"+scores.size());
    }
}
