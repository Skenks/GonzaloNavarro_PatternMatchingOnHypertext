import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("summary.txt", true));
        String[] graphNames = {"ref10000_linear.gfa", "ref10000_snp.gfa", "ref10000_tangle.gfa",
                "ref10000_twopath.gfa"};
        String fastqName = "ref10000_simulatedreads.fastq";

        long startTime = System.nanoTime();
        for (String graphName : graphNames) {
            run_for_one_graph(graphName, fastqName, writer);
        }
        long endTime = System.nanoTime();

        double total_bit_parallel_time_in_seconds = Utility.add_bit_parallel_times(graphNames);
        double our_total_time = (double)(endTime - startTime) / 1000000000;
        double ratio = our_total_time / total_bit_parallel_time_in_seconds;

        System.out.println("Total:");
        System.out.println("bit-parallel took: " + total_bit_parallel_time_in_seconds + "s");
        System.out.println("Navarro implementation took: " + our_total_time + "s");
        System.out.println("time ratio: " + ratio);
        System.out.println();

        writer.append("Total:\n");
        writer.append("bit-parallel took " + total_bit_parallel_time_in_seconds + " s\n");
        writer.append("Navarro took " + (endTime - startTime) / 1000000000 + " s\n");
        writer.append("time ratio: " + ratio + "\n");
        writer.close();
    }

    public static void run_for_one_graph(String graphName, String fastqName, BufferedWriter writer) {

        long startTime = System.nanoTime();

        String bit_parallel_file = graphName.substring(0, graphName.length() - 3) + "txt";
        Path bit_parallel_scores_file = Path.of("bit_parallel_scores/" + bit_parallel_file);
        Path bit_parallel_time_file = Path.of("bit_parallel_times/" + bit_parallel_file);
        Path bit_parallel_memory_file = Path.of("bit_parallel_memory_usage/" + bit_parallel_file);
        Graph graph = Utility.readGraph(Path.of("graphs/" + graphName));
        List<String> fastqs = Utility.readFastq(Path.of("graphs/" + fastqName));
        List<Integer> bit_parallel_scores = Utility.loadScores(bit_parallel_scores_file);
        int bit_parallel_time = Utility.loadTime(bit_parallel_time_file);
        int bit_parallel_memory_usage = Utility.loadMemory(bit_parallel_memory_file);

        boolean hasCycle = false;
        List<Integer> topologicalOrder = null;
        try {
            topologicalOrder = graph.topologicalOrderOfNodesAcyclic();
        } catch (GraphCycleException e) {
            hasCycle = true;
            topologicalOrder = graph.topologicalOrderOfNodesCyclic();
        }

        Aligner aligner = new Aligner(graph, topologicalOrder);
        List<Integer> scores = new ArrayList<>();
        int i = 0;
        System.out.println("out score/bit-parallel score");
        for (String fastq : fastqs) {
            int score = 0;
            if (!hasCycle) {
                score = aligner.align(fastq);
            } else
                score = aligner.alignCycle(fastq);
            System.out.println(score + "/" + bit_parallel_scores.get(i));
            scores.add(score);
            i++;
        }

        long endTime = System.nanoTime();

        int correct = 0;
        for (i = 0; i < scores.size(); i++) {
            if (scores.get(i).equals(bit_parallel_scores.get(i)))
                correct++;
        }

        System.out.println("graph file: " + graphName);
        System.out.println("score: " + correct + "/" + scores.size());
        double bit_parallel_time_in_seconds = bit_parallel_time / 1000000.0;
        double our_time_in_seconds = (endTime - startTime) / 1000000000.0;
        double time_ratio = our_time_in_seconds / bit_parallel_time_in_seconds;
        double bit_parallel_memory_usage_in_kb = bit_parallel_memory_usage / 1024;
        double navarro_usage_in_kb = (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.0;
        double memory_ratio = navarro_usage_in_kb / bit_parallel_memory_usage_in_kb;
        System.out.println("bit parallel took: " + bit_parallel_time_in_seconds + "s");
        System.out.println("Navarro implementation took: " + our_time_in_seconds + "s");
        System.out.println("time ratio: " + time_ratio);
        System.out.println("bit parallel memory usage: " + bit_parallel_memory_usage / 1024 + "KB");
        System.out.println("Navarro memory usage: " + (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + "KB");
        System.out.println("memory ratio: " + memory_ratio);
        System.out.println();
        try {
            writer.append(graphName + ":\n");
            writer.append("score: " + correct + "/" + scores.size() + "\n");
            writer.append("bit parallel took " + bit_parallel_time_in_seconds + " s\n");
            writer.append("Navarro took " + our_time_in_seconds + " s\n");
            writer.append("time ratio: " + time_ratio+ "\n");
            writer.append("bit parallel memory usage: " + (double) bit_parallel_memory_usage / 1024 + "KB\n");
            writer.append("Navarro memory usage: " + (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + "KB\n");
            writer.append("memory ratio: " + memory_ratio + "\n\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
