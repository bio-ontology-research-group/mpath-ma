import sys
import os
import networkx as nx
import rdflib
from rdflib import RDF, RDFS, OWL, BNode, URIRef


def format_class_name(iri):
    # Mimic MakeFuncStructure.groovy
    s = str(iri)
    if s == "http://www.w3.org/2002/07/owl#Thing":
        return "owl:Thing"

    s = s.replace("http://purl.obolibrary.org/obo/", "")
    s = s.replace(">", "")
    # s = s.replace("_", ":") # Groovy script does this

    if "/" in s:
        s = s.split("/")[-1]
    if "#" in s:
        s = s.split("#")[-1]

    return s


def parse_owl_rdflib(owl_file):
    g = rdflib.Graph()
    print(f"Parsing {owl_file}...")
    g.parse(owl_file)
    print("Graph parsed.")

    terms = {}  # IRI -> Label
    graph = nx.DiGraph()

    # 1. Get Labels
    for s, p, o in g.triples((None, RDFS.label, None)):
        if isinstance(s, URIRef):
            terms[str(s)] = str(o)
            graph.add_node(str(s))

    # Ensure all classes are in graph
    for s in g.subjects(RDF.type, OWL.Class):
        if isinstance(s, URIRef):
            if str(s) not in terms:
                terms[str(s)] = str(s)
            graph.add_node(str(s))

    # 2. SubClassOf
    for s, p, o in g.triples((None, RDFS.subClassOf, None)):
        if isinstance(s, URIRef) and isinstance(o, URIRef):
            graph.add_edge(str(o), str(s))  # Parent -> Child

    # 3. EquivalentClasses
    # Case A: C equivalentTo D (Named Class)
    for s, p, o in g.triples((None, OWL.equivalentClass, None)):
        if isinstance(s, URIRef) and isinstance(o, URIRef):
            graph.add_edge(str(o), str(s))
            graph.add_edge(str(s), str(o))

    # Case B: C equivalentTo IntersectionOf (...)
    # ... (existing code) ...

    # 4. Ensure connectivity to owl:Thing
    # Find nodes with in-degree 0 (roots of the provided graph)
    # and link them to owl:Thing
    roots = [n for n in graph.nodes() if graph.in_degree(n) == 0]
    owl_thing = "owl:Thing"  # Identifier for Thing
    # Check if we used full IRI for Thing in graph?
    # In parse, we added nodes as IRIs.
    # owl:Thing IRI is http://www.w3.org/2002/07/owl#Thing
    owl_thing_iri = str(OWL.Thing)

    # If OWL.Thing is not in graph, add it
    if owl_thing_iri not in graph:
        graph.add_node(owl_thing_iri)
        terms[owl_thing_iri] = "owl:Thing"

    for node in roots:
        if node == owl_thing_iri:
            continue
        graph.add_edge(owl_thing_iri, node)

    return terms, graph


def write_func_files(terms, graph, outdir):
    if not os.path.exists(outdir):
        os.makedirs(outdir)

    term_file = os.path.join(outdir, "term.txt")
    term2term_file = os.path.join(outdir, "term2term.txt")
    graph_path_file = os.path.join(outdir, "graph_path.txt")

    print("Writing term.txt...")
    with open(term_file, "w") as f:
        # Root (Thing)
        # Using owl:Thing as root
        f.write("owl:Thing\towl:Thing\towl:Thing\towl:Thing\tend\n")

        for iri, label in terms.items():
            cid = format_class_name(iri)
            f.write(f"{cid}\t{label}\towl:Thing\t{cid}\tend\n")

    print("Writing term2term.txt...")
    counter = 0
    with open(term2term_file, "w") as f:
        for parent, child in graph.edges():
            pid = format_class_name(parent)
            cid = format_class_name(child)
            f.write(f"{counter}\tis-a\t{pid}\t{cid}\n")
            counter += 1

    print("Computing transitive closure...")
    # This can be slow.
    # Use networkx
    tc = nx.transitive_closure(graph)

    print("Writing graph_path.txt...")
    counter = 0
    with open(graph_path_file, "w") as f:
        # Reflexive
        for node in graph.nodes():
            nid = format_class_name(node)
            f.write(f"{counter}\t{nid}\t{nid}\tis-a\t0\t0\n")
            counter += 1

        for ancestor, descendant in tc.edges():
            if ancestor == descendant:
                continue
            aid = format_class_name(ancestor)
            did = format_class_name(descendant)
            f.write(f"{counter}\t{aid}\t{did}\tis-a\t0\t0\n")
            counter += 1

    print("Done.")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python make_func_structure_py.py <owl_file> <out_dir>")
        sys.exit(1)

    owl_file = sys.argv[1]
    out_dir = sys.argv[2]

    terms, graph = parse_owl_rdflib(owl_file)
    write_func_files(terms, graph, out_dir)
