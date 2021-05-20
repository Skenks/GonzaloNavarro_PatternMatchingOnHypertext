import itertools
import gfapy


# Node of a graph
class Node:
    # list of predecessor nodes
    e = []
    # list of successor nodes
    succ = []
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
        self.succ = []
        self.e = []
        self.c1 = 0
        self.c2 = 0
        self.split = False
        self.split_list = []

    def set_e(self, value):
        self.e.append(value)

    def set_succ(self, value):
        self.succ.append(value)

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

    def get_e(self):
        return self.e

    def get_data(self):
        return self.data

    def get_name(self):
        return self.name

    def get_split(self):
        return self.split

    def get_split_list(self):
        return self.split_list

    def __str__(self):

        output_str = self.name
        output_str += '-->'
        for x in self.succ:
            output_str += ";" + str(x)

        return output_str


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
    print(link)

    for k, v in link.items():
        # splitanje oblika 1 -> [
        for value in v:
            graph[int(k) - 1].set_succ(value)
            n = graph[int(k) - 1]
            print(n)

    return graph, 'bbbb'


# g(v,i) function
def g(v, i, patt):
    # find min Cu/(u,v)
    minimal = v.get_e()[0].get_c1()
    for x in v.get_e():
        if x.get_c1() > minimal:
            minimal = x

    if patt[i] == v.get_data():
        return min(minimal, i - 1)

    else:
        return 1 + min(v.get_c1(), minimal)


# find vertices which need reducing, propagate the error through its successors
def propagate(v, u):
    # find vertice to reduce
    if v.get_c1() > 1 + u.get_c1():
        v.set_c1(1 + u.get_c1())
        # propagate error
        for z in v.get_succ():
            propagate(v, z)


# approximate string(pattern) matching on hypertext(graph)
def Search(graph, patt):
    m = len(patt)
    i = 1
    while i < m:
        for v in graph:
            v.set_c2(g(v, i, patt))
        for v in graph:
            v.set_c1(v.get_c2())

        # for all (u, v) e E, Propagate (u,v)
        for v in graph:
            for V, u in itertools.product(v, v.get_e()):
                propagate(V, u)
        i += 1


def main():

    #graph -> oredered list of Vertices
    #patt -> string

    graph, patt = process_data()

    for ggg in graph:
        print(ggg)
        print()
    # gfa()
    # Search(graph, patt)


main()
