import pandas as pd
import networkx as nx
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
import os
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.decomposition import PCA
from sklearn.manifold import MDS, TSNE
from sklearn.preprocessing import StandardScaler
from scipy.cluster import hierarchy
import glob

# Configuration
DATA_FILE = "data/cleaned_with_pamt.tsv"
ONTOLOGY_DIR = "func_data_pamt"
OUTPUT_DIR = "analysis/weighted_semsim"

# Setup Plotting
sns.set_theme(style="white")
plt.rcParams["figure.figsize"] = (14, 12)


def clean_iri(x):
    s = str(x)
    s = s.replace("http://purl.obolibrary.org/obo/", "")
    s = s.replace(">", "")
    if "/" in s:
        s = s.split("/")[-1]
    if "#" in s:
        s = s.split("#")[-1]
    return s


def load_ontology():
    print("Loading ontology structure...")
    term2term_file = os.path.join(ONTOLOGY_DIR, "term2term.txt")
    g = nx.DiGraph()
    with open(term2term_file, "r") as f:
        for line in f:
            parts = line.strip().split("\t")
            if len(parts) >= 4:
                # Format: counter \t is-a \t PARENT \t CHILD
                parent = parts[2]
                child = parts[3]
                g.add_edge(child, parent)  # Edge: Child -> Parent (is-a)
    return g


def get_ancestors_cache(g):
    # Precompute all ancestors for all nodes
    # Returns dict: node -> set(ancestors including self)
    # Using transitive closure or simple BFS/DFS from each node is slow if big.
    # Since it's a DAG, memoization works well.
    memo = {}

    def get_anc(n):
        if n in memo:
            return memo[n]
        anc = {n}
        if n in g:
            for p in g.successors(n):  # Successors are Parents in Child->Parent graph
                anc.update(get_anc(p))
        memo[n] = anc
        return anc

    nodes = list(g.nodes())
    for n in nodes:
        get_anc(n)

    return memo


def compute_strain_vectors(df, ancestors_map):
    # 1. Aggregate terms per strain
    # We need: Strain -> List of Terms (one per animal observation)
    # Actually, we treat each row as an observation (lesion).
    # If one animal has 3 lesions, they contribute to the strain's profile.
    # Relative frequency = (Count of Term X in Strain) / (Total Lesions in Strain)
    # OR / (Total Animals in Strain)?
    # Usually "Prevalence" implies per Animal.
    # Let's count "Animals with Term X" / "Total Animals".

    # Group by Strain -> Animal -> Terms
    strain_animal_terms = {}

    for idx, row in df.iterrows():
        strain = str(row["Strain"])
        mid = str(row["new IDs"])
        val = row["PAMT_IRI"]

        if pd.isna(val) or str(val) == "nan":
            continue
        term = clean_iri(val)

        if strain not in strain_animal_terms:
            strain_animal_terms[strain] = {}
        if mid not in strain_animal_terms[strain]:
            strain_animal_terms[strain][mid] = set()

        strain_animal_terms[strain][mid].add(term)

    # Build Feature Matrix
    # Rows: Strains
    # Cols: All Ontology Terms (that appear or are ancestors)

    # 1. Identify all used terms and their ancestors
    all_used_terms = set()
    for s_dict in strain_animal_terms.values():
        for terms in s_dict.values():
            all_used_terms.update(terms)

    # Expand to all ancestors
    all_features = set()
    for t in all_used_terms:
        if t in ancestors_map:
            all_features.update(ancestors_map[t])
        else:
            all_features.add(t)  # Term not in ontology? Keep it as is.

    feature_list = sorted(list(all_features))
    feature_idx = {t: i for i, t in enumerate(feature_list)}

    strains = sorted(list(strain_animal_terms.keys()))
    n_strains = len(strains)
    n_features = len(feature_list)

    matrix = np.zeros((n_strains, n_features))

    for i, strain in enumerate(strains):
        animals = strain_animal_terms[strain]
        n_animals = len(animals)
        if n_animals == 0:
            continue

        # Vector sum of all animals
        # For each animal, propagate terms
        # If animal has T, it implies it has all ancestors of T.
        # We want prevalence: % of animals having T (or subtype of T).

        counts = np.zeros(n_features)

        for mid, terms in animals.items():
            # Get full set of terms for this animal (including ancestors)
            animal_profile = set()
            for t in terms:
                if t in ancestors_map:
                    animal_profile.update(ancestors_map[t])
                else:
                    animal_profile.add(t)

            # Add to counts
            for t in animal_profile:
                if t in feature_idx:
                    counts[feature_idx[t]] += 1

        # Normalize
        matrix[i, :] = counts / n_animals

    return strains, feature_list, matrix


def analyze_subset(df, subset_name, ancestors_map):
    print(f"--- Processing {subset_name} ---")
    out_dir = os.path.join(OUTPUT_DIR, subset_name)
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)

    strains, features, X = compute_strain_vectors(df, ancestors_map)

    if len(strains) < 2:
        print("  Not enough strains.")
        return

    # 1. Cosine Similarity
    sim_matrix = cosine_similarity(X)
    df_sim = pd.DataFrame(sim_matrix, index=strains, columns=strains)
    df_sim.to_csv(os.path.join(out_dir, "weighted_similarity.csv"))

    # 2. Clustered Heatmap
    try:
        g = sns.clustermap(
            df_sim,
            method="average",
            metric="euclidean",  # Clustering on sim profiles
            cmap="viridis",
            figsize=(14, 14),
            linewidths=0.5,
            dendrogram_ratio=(0.15, 0.15),
            cbar_pos=(0.02, 0.8, 0.03, 0.15),
        )
        g.ax_heatmap.set_title(
            f"Weighted Similarity (Cosine TPR) - {subset_name}", pad=100
        )
        plt.setp(g.ax_heatmap.get_xticklabels(), rotation=45, ha="right")
        plt.setp(g.ax_heatmap.get_yticklabels(), rotation=0)
        g.savefig(os.path.join(out_dir, "heatmap_clustered.png"))
        plt.close()
    except Exception as e:
        print(f"  Heatmap failed: {e}")

    # 3. PCA
    try:
        pca = PCA(n_components=2)
        X_pca = pca.fit_transform(
            X
        )  # PCA on the Feature Matrix (Propagated Frequencies), NOT Sim Matrix

        plt.figure(figsize=(10, 8))
        sns.scatterplot(x=X_pca[:, 0], y=X_pca[:, 1], s=100)
        for i, s in enumerate(strains):
            plt.text(X_pca[i, 0] + 0.01, X_pca[i, 1] + 0.01, s, fontsize=9)
        plt.title(f"PCA (on Propagated Frequencies) - {subset_name}")
        plt.xlabel("PC1")
        plt.ylabel("PC2")
        plt.tight_layout()
        plt.savefig(os.path.join(out_dir, "pca.png"))
        plt.close()
    except Exception as e:
        print(f"  PCA failed: {e}")


def main():
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)

    # Load Ontology & Cache Ancestors
    g = load_ontology()
    print("Computing transitive closure (ancestors)...")
    ancestors_map = get_ancestors_cache(g)
    print("Done.")

    # Load Data
    df = pd.read_csv(DATA_FILE, sep="\t")
    df.columns = [c.strip() for c in df.columns]

    # Subsets
    # 1. Global
    analyze_subset(df, "Global", ancestors_map)

    # 2. Sex
    for sex in df["Sex"].dropna().unique():
        analyze_subset(df[df["Sex"] == sex], f"Sex_{sex}", ancestors_map)

    # 3. Code
    for code in df["Code"].dropna().unique():
        analyze_subset(df[df["Code"] == code], f"Code_{code}", ancestors_map)

    # 4. Sex + Code
    for sex in df["Sex"].dropna().unique():
        for code in df["Code"].dropna().unique():
            sub = df[(df["Sex"] == sex) & (df["Code"] == code)]
            analyze_subset(sub, f"Sex_{sex}_Code_{code}", ancestors_map)

    print(f"\nWeighted analysis complete. Results in {OUTPUT_DIR}/")


if __name__ == "__main__":
    main()
