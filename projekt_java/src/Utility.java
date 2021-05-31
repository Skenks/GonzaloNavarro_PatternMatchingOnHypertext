import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains utility functions.
 */
public class Utility {

    /**
     * Reads graph from .gfa file and creates Graph
     *
     * @param filePath path of .gfa file
     * @return created graph
     */
    public static Graph readGraph(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            Graph graph = new Graph();

            //read overlap if it exists and make sure it is same everywhere
            for (String line : lines) {
                if (line.charAt(0) == 'L') {
                    String overlap_oznaka = line.strip().split("\t")[5];
                    overlap_oznaka = overlap_oznaka.substring(0, overlap_oznaka.length() - 1);
                    int overlap = overlap_oznaka.equals("") ? 0 : Integer.parseInt(overlap_oznaka);
                    assert graph.overlap == 0 || graph.overlap == overlap;
                    graph.overlap = overlap;
                }
            }

            for (String line : lines) {
                if (line.charAt(0) == 'S') {
                    Node first_node = createNode(line, graph.overlap);
                    Node reverse_complement_node = createReverseComplementNode(line, graph.overlap);
                    graph.addNode(first_node);
                    graph.addNode(reverse_complement_node);
                }
            }

            for (String line : lines) {
                if (line.charAt(0) == 'L') {
                    graph.createAndAddEdges(line);
                }
            }

            return graph;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Reads fastq sequences from file and returns them in list.
     *
     * @param filePath path of fastq file
     * @return list of fastq sequences
     */
    public static List<String> readFastq(Path filePath) {
        List<String> fastqs = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(filePath);
            int i = 0;
            while (i < lines.size()) {
                String line = lines.get(i).strip();
                if (line.charAt(0) != '@') {
                    i++;
                    continue;
                }
                i++;
                line = lines.get(i).strip();
                String sequence = line;
                i += 2;
                fastqs.add(sequence);
            }
            return fastqs;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads bit-parallel scores in list and returns it.
     *
     * @param filePath path of file containing scores
     * @return list of scores
     */
    public static List<Integer> loadScores(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            List<Integer> scores = new ArrayList<>();
            for (String line : lines) {
                line = line.strip();
                scores.add(Integer.parseInt(line));
            }
            return scores;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Pareses line and creates normal node with overlap if overlap exists.
     * @param line
     * @param overlap
     * @return
     */
    public static Node createNode(String line, int overlap) {
        String[] arr = line.split("\t");
        int id = Integer.parseInt(arr[1]);
        String sequence = arr[2];
        //for every node from file we are creating two nodes - one normal (id * 2) and one reverse complement (id * 2 + 1)
        return new Node(id * 2, false, sequence.substring(0, sequence.length() - overlap));
    }

    /**
     * Pareses line and creates reverse complement node with overlap if overlap exists.
     * @param line
     * @param overlap
     * @return
     */
    public static Node createReverseComplementNode(String line, int overlap) {
        String[] arr = line.split("\t");
        int id = Integer.parseInt(arr[1]);
        String sequence = arr[2];
        //for every node from file we are creating two nodes - one normal (id * 2) and one reverse complement (id * 2 + 1)
        return new Node(id * 2 + 1, true, getReverseComplement(sequence).substring(0, sequence.length() - overlap));
    }

    /**
     * Returns reverse complement of sequence.
     * @param sequence
     * @return
     */
    public static String getReverseComplement(String sequence) {
        StringBuilder sb = new StringBuilder();
        char[] arr = sequence.toCharArray();
        for (int i = arr.length - 1; i >= 0; i--) {
            char c = switch (arr[i]) {
                case 'A' -> 'T';
                case 'C' -> 'G';
                case 'G' -> 'C';
                case 'T' -> 'A';
                default -> throw new IllegalStateException("Unexpected value: " + arr[i]);
            };
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Loads bit-parallel time (in microseconds) and returns it.
     * @param
     * @return
     */
    public static Integer loadTime(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            return Integer.parseInt(lines.get(0).trim());
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static double add_bit_parallel_times(String[] graphNames) {
        double sum =  0.0;
        for (String graphName : graphNames) {
            sum += loadTime(Path.of("../bit_parallel_times/" + graphName.substring(0, graphName.length()-3) + "txt"));
        }
        return sum/1000000;
    }

    public static int loadMemory(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            return Integer.parseInt(lines.get(0).trim());
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Copies List<List<Integer>> and returns copy.
     * @param neighbors
     * @return
     */
    public static List<List<Integer>> copyNeighbors(List<List<Integer>> neighbors) {
        List<List<Integer>> newList = new ArrayList<>();
        for (List<Integer> nodeNeighbors : neighbors) {
            newList.add(new ArrayList<>(nodeNeighbors));
        }
        return newList;
    }
}
