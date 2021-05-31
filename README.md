# GonzaloNavarro_PatternMatchingOnHypertext
Improved approximate pattern matching on hypertext

Commands for running Navarro algorithm on 4 topologicaly different graphs (linear, snp, tangle, twopath):

- cd projekt_java/src
- javac Main.java
- java Main	


Each graph is aligned with 77 patterns from ref10000_simulatedreads.fastq file.
Graphs are constructed from the first 10000 bp of E.Coli reference genome.
Results (compared to bit parallel algorithm) are in projekt_java/src/summary.txt.
