import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Utility {

    public static Graph readGraph(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            Graph graph = new Graph();

            //procitaj overlap ako postoji i provjeri da je svugdje isti
            for (String line : lines) {
                if (line.charAt(0) == 'L') {
                    //svi primjeri imaju jednaki overlap za svaki redak, ako postoji
                    String overlap_oznaka = line.strip().split("\t")[5];
                    overlap_oznaka = overlap_oznaka.substring(0, overlap_oznaka.length() - 1);
                    int overlap = overlap_oznaka.equals("") ? 0 : Integer.parseInt(overlap_oznaka);
                    assert graph.getOverlap() == 0 || graph.getOverlap() == overlap;
                    graph.setOverlap(overlap);
                }
            }

            for (String line : lines) {
                if (line.charAt(0) == 'S') {
                    Node first_node = createNode(line, graph.getOverlap());
                    Node reverse_complement_node = createReverseComplementNode(line, graph.getOverlap());
                    graph.addNode(first_node);
                    graph.addNode(reverse_complement_node);
                }
            }

            for (String line : lines) {
                if (line.charAt(0) == 'L') {
                    graph.createAndAddEdges(line);
                }
            }

            graph.summary();
            return graph;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static List<FastQ> readFastq(Path filePath) {
        List<FastQ> fastqs = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(filePath);
            int i = 0;
            while(i < lines.size()) {
                String line = lines.get(i).strip();
                if (line.charAt(0) != '@') {
                    i++;
                    continue;
                }
                FastQ fastq = new FastQ();
                fastq.id = (line.substring(1));
                i++;
                line = lines.get(i).strip();
                fastq.sequence = line;
                i += 2;
                fastqs.add(fastq);
            }
            return fastqs;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

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

        public static Node createNode(String line, int overlap) {
        String[] arr = line.split("\t");
        int id = Integer.parseInt(arr[1]);
        String sequence = arr[2];
        return new Node(id * 2, false, sequence.substring(0, sequence.length() - overlap));

    }

    public static Node createReverseComplementNode(String line, int overlap) {
        String[] arr = line.split("\t");
        int id = Integer.parseInt(arr[1]);
        String sequence = arr[2];
        return new Node(id * 2 + 1, true, getReverseComplement(sequence).substring(0, sequence.length() - overlap));

    }

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

    public static List<List<Integer>> copyNeighbors(List<List<Integer>> neighbors) {
        List<List<Integer>> newList = new ArrayList<>();
        for (List<Integer> nodeNeighbors : neighbors) {
            newList.add(new ArrayList<>(nodeNeighbors));
        }
        return newList;
    }
}
