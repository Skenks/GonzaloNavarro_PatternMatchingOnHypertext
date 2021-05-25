import itertools
import sys

import gfapy


# Node of a graph
class Node:
    # list of in nodes
    in_nodes = []
    # list of out nodes
    out_nodes = []
    # name - number
    name = ''
    # C value
    c1 = 0
    # C' value
    c2 = 0
    # char data of a node
    data = ''
    # split = boolean
    split = False
    # split_list -> if len(data) > 1 -> split string into nodes
    split_list = []

    def __init__(self, name, data):
        self.name = name
        self.data = data
        self.out_nodes = []
        self.in_nodes = []
        self.c1 = 0
        self.c2 = 0
        self.split = False
        self.split_list = []

    def set_in_nodes(self, value):
        self.in_nodes.append(value)

    def set_out_nodes(self, value):
        self.out_nodes.append(value)

    def set_name(self, value):
        self.name = value

    def set_c1(self, value):
        self.c1 = value

    def set_c2(self, value):
        self.c2 = value

    def set_data(self, value):
        self.data = value

    def set_split(self, value):
        self.split = value

    def get_c1(self):
        return self.c1

    def get_c2(self):
        return self.c2

    def get_in_nodes(self):
        return self.in_nodes

    def get_out_nodes(self):
        return self.out_nodes

    def get_data(self):
        return self.data

    def get_name(self):
        return self.name

    def get_split(self):
        return self.split

    def get_split_list(self):
        return self.split_list

    def structure_print(self):
        output_str = '[NODE]=('
        output_str += self.name + ') '
        output_str += '[OUT:]'

        for x in self.out_nodes:
            output_str += ";" + str(x)

        output_str += ' [IN:]'

        for x in self.in_nodes:
            output_str += ";" + str(x)

        print(output_str)

        return

    def C_values_print(self):
        print('C = ' + str(self.c1))
        return

    def __str__(self):
        return self.name

def gfa():
    # nikaj to ipak...
    #
    #
    #
    filepath = 'twopath.gfa'
    g = gfapy.Gfa.from_file(filepath)
    # print(str(g))
    print(g.edge_names)


def process_data():
    # data processing...
    file_path = "twopath.gfa"
    file = open(file_path, 'r')
    all_lines = file.readlines()
    graph = []
    link = {}

    for x in all_lines:
        atr = x.strip().split()
        # print(atr)

        if atr[0] == "S":
            node = Node(atr[1], atr[2])
            node.set_data(atr[2])
            graph.append(node)

        if atr[0] == "L":
            if atr[1] in link.keys():
                if atr[3] not in link[atr[1]]:
                    link[atr[1]].append(atr[3])
            else:
                link[atr[1]] = [atr[3]]
    # print(link)

    for k, v in link.items():
        # splitanje oblika 1 -> [
        for value in v:
            graph[int(value) - 1].set_in_nodes(graph[int(k)-1])
            graph[int(k) - 1].set_out_nodes(graph[int(value)-1])

        # n = graph[int(k) - 1]
        # n.structure_print()

    return graph, 'A'


# g(v,i) function
def g(v, i, patt, graph):
    # find min Cu/(u,v)
    v_in_nodes = graph[v].get_in_nodes()

    if v_in_nodes:
        minimal = graph[v_in_nodes[0]].get_c1()

        for x in v_in_nodes:
            if x.get_c1() < minimal:
                minimal = x

    # if v has no in_nodes minimal = MAX -> e.g. starting nodes
    else:
        minimal = sys.float_info.max

    if patt[i-1] == v.get_data():
        return min(minimal, i - 1)

    else:
        return 1 + min(v.get_c1(), minimal)


# find vertices which need reducing, propagate the error through its successors
def propagate(v, u):
    # find vertice to reduce
    if v.get_c1() > 1 + u.get_c1():
        v.set_c1(1 + u.get_c1())
        # propagate error
        for z in v.get_out_nodes():
            propagate(v, z)


# approximate string(pattern) matching on hypertext(graph)
def Search(graph, patt):
    m = len(patt) + 1
    i = 1
    while i < m:
        for v in graph:
            v.set_c2(g(v, i, patt, graph))
        for v in graph:
            v.set_c1(v.get_c2())

        # for all (u, v) e E, Propagate (u,v)
        for v in graph:
            for V, u in itertools.product(v, v.get_in_nodes()):
                propagate(V, u)
        i += 1


def main():

    #graph -> oredered list of Vertices
    #patt -> string

    graph, patt = process_data()

    Search(graph, patt)

    for ggg in graph:
        ggg.structure_print()
        ggg.C_values_print()
    # gfa()
    # Search(graph, patt)


main()