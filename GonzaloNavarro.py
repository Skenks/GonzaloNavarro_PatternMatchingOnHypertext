import itertools


# Vertice of a graph
class Vertice:
    # list of predecessor nodes
    e = []
    # list of successor nodes
    succ = []
    # C value
    c1 = 0
    # C' value
    c2 = 0
    # char data of a node
    data = ''

    def __init__(self, e):
        self.e.append(e)

    def set_c1(self, value):
        self.c1 = value

    def set_c2(self, value):
        self.c2 = value

    def set_data(self, value):
        self.data = value

    def get_c1(self):
        return self.c1

    def get_c2(self):
        return self.c2

    def get_e(self):
        return self.e

    def get_data(self):
        return self.data


def process_data():
    # data processing...
    return {}, 'bbbb'


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

    Search(graph, patt)
