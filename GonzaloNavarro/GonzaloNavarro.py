import argparse
import itertools
import sys
import os


# Node of a graph
class Node:
    # list of in nodes
    in_nodes = []
    # list of out nodes
    out_nodes = []
    # id - number
    node_id = 0
    # original id in gfa file - number
    original_node_id = 0
    # C value
    c1 = 0
    # C' value
    c2 = 0
    # char data of a node
    sequence = ''
    # not_reversed = boolean
    reversed = True
    # split_list -> if len(data) > 1 -> split string into nodes
    split_list = []

    def __init__(self, node_id, original_node_id, is_reversed, sequence):
        self.node_id = node_id
        self.sequence = sequence
        self.reversed = is_reversed
        self.original_node_id = original_node_id


class Edge:
    def __init__(self, from_node, to_node):
        self.from_node = from_node
        self.to_node = to_node


class FastQ:
    def __init__(self):
        self.seq_id = ""
        self.sequence = ""
        self.quality = ""


class AlignmentGraph:
    def __init__(self):
        self.nodeStart = []
        self.nodeOffset = []
        self.nodeIDs = []
        self.nodeLookup = {}
        self.inNeighbors = []
        self.outNeighbors = []
        self.reverse = []
        self.nodeSequencesATorCG = []
        self.nodeSequencesACorTG = []
        self.nodeSequences = []
        self.overlap = 0
        self.max_node_size = 100

    def node_len(self):
        return len(self.nodeStart)

    def sequence_len(self):
        return len(self.nodeSequences)

    def get_sequence_char(self, index):
        return self.nodeSequences[index]

    def node_start_in_seq(self, index):
        return self.nodeStart[index]

    def node_end_in_seq(self, index):
        if index == len(self.nodeStart) - 1:
            return len(self.nodeSequences)
        return self.nodeStart[index + 1]

    def add_node_any_size(self, node):
        node_id = node.node_id
        sequence = node.sequence
        reversed = node.reversed
        for i in range(0, len(sequence), self.max_node_size):
            self.add_node(node_id, i, sequence[i:i + self.max_node_size], reversed)
            if i > 0:
                # print(f"split: {node_id} {len(sequence)}")
                self.outNeighbors[len(self.outNeighbors) - 2].append(len(self.outNeighbors) - 1)
                self.inNeighbors[len(self.inNeighbors) - 1].append(len(self.inNeighbors) - 2)

    def add_node(self, node_id, offset, sequence, reversed):
        if node_id not in self.nodeLookup.keys():
            self.nodeLookup[node_id] = []
        self.nodeLookup[node_id].append(len(self.nodeStart))
        self.nodeIDs.append(node_id)
        self.nodeStart.append(len(self.nodeSequences))
        self.reverse.append(reversed)
        self.nodeOffset.append(offset)
        self.inNeighbors.append([])
        self.outNeighbors.append([])
        for c in sequence:
            self.nodeSequences += c

    def add_edge(self, edge):
        from_id = edge.from_node
        to_id = edge.to_node
        from_node = self.nodeLookup[from_id][-1]
        to_node = self.nodeLookup[to_id][0]

        # don't add double edges

        if not self.inNeighbors[to_node].__contains__(from_node):
            self.inNeighbors[to_node].append(from_node)

        if not self.outNeighbors[from_node].__contains__(to_node):
            self.outNeighbors[from_node].append(to_node)

    def print_graph_summary(self):
        print(f"{len(self.nodeLookup)} original nodes")
        print(f"{len(self.nodeStart)} split nodes")
        print(f"{len(self.nodeSequences)} bp")
        special_nodes = 0
        edges = 0
        for i in range(len(self.inNeighbors)):
            if len(self.inNeighbors[i]) >= 2:
                special_nodes += 1
            edges += len(self.inNeighbors[i])
        print(f"{edges} edges")
        print(f"{special_nodes} nodes with in-degree >= 2")

    def topological_order_of_components(self):
        index = [-1] * self.node_len()
        lowlink = [-1] * self.node_len()
        onStack = [False] * self.node_len()
        S = []
        indexnum = 0
        result = []
        for i in range(self.node_len()):
            if index[i] == -1:
                self.connect(i, result, indexnum, index, lowlink, onStack, S)
        result.reverse()
        belongs_to_component = [-1] * self.node_len()
        for component in range(len(result)):
            for node in result[component]:
                belongs_to_component[node] = component
        return result, belongs_to_component

    def connect(self, node, result, indexnum, index, lowlink, onStack, S):
        index[node] = indexnum
        lowlink[node] = indexnum
        indexnum += 1
        S.append(node)
        onStack[node] = True
        #print(node)
        for neighbor in self.inNeighbors[node]:
            if index[neighbor] == -1:
                self.connect(neighbor, result, indexnum, index, lowlink, onStack, S)
                lowlink[node] = min(lowlink[neighbor], lowlink[node])
            elif onStack[neighbor]:
                lowlink[node] = min(lowlink[node], index[neighbor])

        if lowlink[node] == index[node]:
            result.append([])
            while True:
                #sve koje si do tad dodao na stog nakon izvornog node-a stavi u result[-1]
                w = S.pop()
                onStack[w] = False
                result[-1].append(w)
                if w == node:
                    break


def create_nodes(line, overlap):
    arr = line.split("\t")
    id_ = int(arr[1])
    sequence = arr[2]
    first = Node(node_id=id_ * 2, original_node_id=id_, is_reversed=False,
                 sequence=sequence[0: len(sequence) - overlap])
    second = Node(node_id=id_ * 2 + 1, original_node_id=id_, is_reversed=True,
                  sequence=reverse_complement(sequence[0: len(sequence) - overlap]))
    return first, second


def create_edges(line):
    arr = line.split("\t")
    from_node = int(arr[1])
    from_start = arr[2]
    to_node = int(arr[3])
    to_end = arr[4]
    if from_start == "-":
        from_left = from_node * 2
        from_right = from_node * 2 + 1
    else:
        from_left = from_node * 2 + 1
        from_right = from_node * 2
    if to_end == "-":
        to_left = to_node * 2
        to_right = to_node * 2 + 1
    else:
        to_left = to_node * 2 + 1
        to_right = to_node * 2
    first = Edge(from_right, to_right)
    second = Edge(to_left, from_left)
    return first, second


def reverse_complement(text):
    new_text = ""
    reversed_text = text[::-1]
    for c in reversed_text:
        if c == 'A':
            new_text += 'T'
        if c == 'C':
            new_text += 'G'
        if c == 'T':
            new_text += 'A'
        if c == 'G':
            new_text += 'C'
    return new_text


def load_graph(filepath):
    graph = AlignmentGraph()
    file = open(filepath, 'r')
    all_lines = file.readlines()
    graph.overlap = 0

    for line in all_lines:
        if line[0] == "L":
            overlap_str = line.strip().split("\t")[5]
            overlap_str = overlap_str[0:len(overlap_str) - 1]
            overlap = 0 if overlap_str == "" else int(overlap_str)
            assert graph.overlap == 0 or graph.overlap == overlap
            graph.overlap = overlap

    for line in all_lines:
        if line[0] == 'S':
            first, second = create_nodes(line.strip(), graph.overlap)
            graph.add_node_any_size(node=first)
            graph.add_node_any_size(second)

    for line in all_lines:
        if line[0] == 'L':
            first, second = create_edges(line)
            graph.add_edge(first)
            graph.add_edge(second)

    graph.print_graph_summary()
    return graph


def load_fastq(filepath):
    file1 = open(filepath, 'r')
    count = 0
    fastqs = []

    while True:
        count += 1
        line = file1.readline().strip()
        if not line:
            break
        if line[0] != '@':
            continue
        fastq = FastQ()
        fastq.seq_id = line[1::]
        line = file1.readline().strip()
        fastq.sequence = line
        line = file1.readline()
        line = file1.readline().strip()
        fastq.quality = line
        fastqs.append(fastq)

    file1.close()
    return fastqs


def load_scores(filepath):
    file = open(filepath, 'r')
    all_lines = file.readlines()
    scores = []
    for line in all_lines:
        line = line.strip()
        scores.append(int(line))
    return scores


class Aligner:
    graph = None
    belongs_to_component = None
    component_order = None

    def __init__(self, graph, component_order, belongs_to_component):
        self.graph = graph
        self.belongs_to_component = belongs_to_component
        self.component_order = component_order

    def calculate_acyclic(self, current_slice, previous_slice, pattern, j, node):
        start = self.graph.node_start_in_seq(node)
        end = self.graph.node_end_in_seq(node)
        current_slice[start] = previous_slice[start] + 1
        char_in_graph = self.graph.get_sequence_char(start)
        match = char_in_graph == pattern[j]
        for neighbor in self.graph.inNeighbors[node]:
            u = self.graph.node_end_in_seq(neighbor) - 1  # indeks zadnjeg znaka u susjedu
            current_slice[start] = min(current_slice[start], current_slice[u] + 1,
                                       previous_slice[u] + (0 if match else 1))
        for w in range(start + 1, end):
            char_in_graph = self.graph.get_sequence_char(w)
            match = char_in_graph == pattern[j]
            current_slice[w] = min(current_slice[w - 1] + 1, previous_slice[w] + 1,
                                   previous_slice[w - 1] + (0 if match else 1))

    def calculate_cyclic_component(self, pattern, nodes, current_slice, previous_slice, j):

        for node in nodes:
            self.calculate_acyclic(current_slice, previous_slice, pattern, j, node)

        for node in nodes:
            start = self.graph.node_start_in_seq(node)
            for neighbor in self.graph.inNeighbors[node]:
                u = self.graph.node_start_in_seq(neighbor) - 1
                if current_slice[start] > current_slice[u] + 1:
                    self.recurse_horizontal_scores(current_slice, node, start, current_slice[u] + 1)
            for w in range(start + 1, self.graph.node_end_in_seq(node)):
                if current_slice[w] > current_slice[w - 1] + 1:
                    self.recurse_horizontal_scores(current_slice, node, w, current_slice[w - 1] + 1)

    def recurse_horizontal_scores(self, current_slice, node, start, new_score):
        current_slice[start] = new_score
        new_score += 1
        for w in range(start + 1, self.graph.node_end_in_seq(node)):
            if current_slice[w] <= new_score:
                return
            current_slice[w] = new_score
            new_score += 1
        for neighbor in self.graph.outNeighbors[node]:
            if self.belongs_to_component[neighbor] == self.belongs_to_component[node]:
                u = self.graph.node_start_in_seq(neighbor)
                if current_slice[u] > new_score:
                    self.recurse_horizontal_scores(current_slice, neighbor, u, new_score)

    # approximate string(pattern) matching on hypertext(graph)
    def align(self, pattern):
        current_slice = self.graph.sequence_len() * [0]
        previous_slice = self.graph.sequence_len() * [0]

        # inicijalizacija previous_slicea
        for w in range(len(previous_slice)):
            char_in_graph = self.graph.get_sequence_char(w)
            previous_slice[w] = 0 if pattern[0] == char_in_graph else 1

        for j in range(1, len(pattern)):
            for i in range(len(self.component_order)):
                self.calculate_acyclic(current_slice, previous_slice, pattern, j, self.component_order[i][0])

            current_slice, previous_slice = previous_slice, current_slice

        najmanji = sys.maxsize

        for i in range(len(previous_slice)):
            najmanji = min(najmanji, previous_slice[i])

        return najmanji

    def align_cyclic(self, pattern):
        current_slice = self.graph.sequence_len() * [0]
        previous_slice = self.graph.sequence_len() * [0]

        # inicijalizacija previous_slicea
        for w in range(len(previous_slice)):
            char_in_graph = self.graph.get_sequence_char(w)
            previous_slice[w] = 0 if pattern[0] == char_in_graph else 1

        for j in range(1, len(pattern)):
            # kopiraj pattern string u slice
            current_slice = [len(pattern)] * len(current_slice)
            for component in self.component_order:
                if len(component) == 1:
                    isAcyclic = True
                    for neighbor in self.graph.inNeighbors[component[0]]:
                        if neighbor == component[0]:
                            isAcyclic = False
                            break
                    if isAcyclic:
                        self.calculate_acyclic(current_slice, previous_slice, pattern, j, component[0])
                        continue
                self.calculate_cyclic_component(pattern, component, current_slice, previous_slice, j)
            current_slice, previous_slice = previous_slice, current_slice

        najmanji = sys.maxsize
        for i in range(len(previous_slice)):
            najmanji = min(najmanji, previous_slice[i])

        return najmanji


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '-f',
        help='Ime datoteke patterna',
        default="ref10000_simulatedreads.fastq"
    )
    parser.add_argument(
        '-g',
        help='Ime datoteke grafa',
        default='ref10000_tangle.gfa',
        choices=['ref10000_linear.gfa', 'ref10000_snp.gfa', 'ref10000_twopath.gfa', 'ref10000_onechar.gfa',
                 'ref10000_tangle.gfa']
    )

    args = parser.parse_args()
    current_dir = os.getcwd()
    sys.setrecursionlimit(30000)

    fastqfile = os.path.join(current_dir, 'tmp', args.f)
    graphfile = os.path.join(current_dir, 'tmp', args.g)
    scores_file = args.g[:-3] + 'txt'
    bit_parallel_file = os.path.join(current_dir, 'bit_parallel_scores', scores_file)

    alignmentGraph = load_graph(graphfile)
    fastqs = load_fastq(fastqfile)
    bit_parallel_scores = load_scores(bit_parallel_file)

    componentOrder = alignmentGraph.topological_order_of_components()
    first, second = componentOrder  # result, belongsToCOmponent
    isAcyclic = True
    isTree = True
    for i in range(len(first)):

        if len(first[i]) > 1:
            isAcyclic = False
            break

        for neighbor in alignmentGraph.inNeighbors[first[i][0]]:
            if neighbor == first[i][0]:
                isAcyclic = False
                break

    if isAcyclic:
        for i in range(alignmentGraph.node_len()):
            if len(alignmentGraph.inNeighbors[i]) > 1:
                isTree = False
                break
    else:
        isTree = False

    if isTree:
        print("The graph is linear / a tree / a forest")
    elif isAcyclic:
        print("The graph is a DAG")
    else:
        print("The graph is cyclic")

    aligner = Aligner(graph=alignmentGraph, component_order=first, belongs_to_component=second)
    scores = []
    for fastq in fastqs:
        score = 0
        if isAcyclic:
            score = aligner.align(pattern=fastq.sequence)
            scores.append(score)
            print(score)
        else:
            score = aligner.align_cyclic(pattern=fastq.sequence)
            scores.append(score)
            print(score)

    # usporedi sa bit parallel
    correct_len = 0
    for i in range(len(scores)):
        if scores[i] == bit_parallel_scores[i]:
            correct_len += 1

    print(f"{correct_len}/{len(scores)}")

main()
