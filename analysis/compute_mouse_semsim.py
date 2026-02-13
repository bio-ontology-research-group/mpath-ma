import pandas as pd
import networkx as nx
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
import os
from sklearn.decomposition import PCA
from sklearn.manifold import TSNE
import glob

# Configuration
DATA_FILE = "data/cleaned_with_pamt.tsv"
ONTOLOGY_DIR = "func_data_pamt"
OUTPUT_DIR = "analysis/mouse_level_semsim"

# Setup Plotting
sns.set_theme(style="whitegrid")
plt.rcParams["figure.figsize"] = (12, 10)


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
                parent = parts[2]
                child = parts[3]
                g.add_edge(child, parent)
    return g


def get_ancestors_cache(g):
    memo = {}

    def get_anc(n):
        if n in memo:
            return memo[n]
        anc = {n}
        if n in g:
            for p in g.successors(n):
                anc.update(get_anc(p))
        memo[n] = anc
        return anc

    nodes = list(g.nodes())
    for n in nodes:
        get_anc(n)
    return memo


def compute_mouse_matrix(df, ancestors_map):
    # Group by Mouse ID
    mouse_terms = {}
    mouse_meta = {}  # ID -> Strain

    for idx, row in df.iterrows():
        mid = str(row["new IDs"])
        strain = str(row["Strain"])
        val = row["PAMT_IRI"]

        if pd.isna(val) or str(val) == "nan":
            continue
        term = clean_iri(val)

        if mid not in mouse_terms:
            mouse_terms[mid] = set()
            mouse_meta[mid] = strain

        # TPR Propagation
        if term in ancestors_map:
            mouse_terms[mid].update(ancestors_map[term])
        else:
            mouse_terms[mid].add(term)

    # Feature Matrix
    all_features = set()
    for terms in mouse_terms.values():
        all_features.update(terms)

    feature_list = sorted(list(all_features))
    feature_idx = {t: i for i, t in enumerate(feature_list)}

    mids = sorted(list(mouse_terms.keys()))
    strains = [mouse_meta[m] for m in mids]

    n_mice = len(mids)
    n_features = len(feature_list)

    X = np.zeros((n_mice, n_features))

    for i, mid in enumerate(mids):
        for t in mouse_terms[mid]:
            if t in feature_idx:
                X[i, feature_idx[t]] = 1.0

    return mids, strains, X


def plot_embedding(X_emb, strains, title, output_path, method_name):
    df_plot = pd.DataFrame(X_emb, columns=["Dim1", "Dim2"])
    df_plot["Strain"] = strains

    plt.figure(figsize=(14, 10))

    # Check number of strains for palette
    n_strains = len(set(strains))
    palette = "tab20" if n_strains <= 20 else "husl"

    sns.scatterplot(
        data=df_plot,
        x="Dim1",
        y="Dim2",
        hue="Strain",
        palette=palette,
        s=60,
        alpha=0.7,
        linewidth=0,
    )

    # Move legend outside if too many
    if n_strains > 10:
        plt.legend(bbox_to_anchor=(1.02, 1), loc="upper left", borderaxespad=0)
    else:
        plt.legend(loc="best")

    plt.title(f"{method_name} - {title} (N={len(strains)})")
    plt.tight_layout()
    plt.savefig(output_path)
    plt.close()


def analyze_subset(df, subset_name, ancestors_map):
    print(f"--- Processing {subset_name} ---")
    out_dir = os.path.join(OUTPUT_DIR, subset_name)
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)

    mids, strains, X = compute_mouse_matrix(df, ancestors_map)

    if len(mids) < 3:
        print("  Not enough mice.")
        return

    # 1. PCA
    try:
        pca = PCA(n_components=2)
        X_pca = pca.fit_transform(X)
        plot_embedding(
            X_pca, strains, subset_name, os.path.join(out_dir, "pca_mouse.png"), "PCA"
        )
    except Exception as e:
        print(f"  PCA failed: {e}")

    # 2. t-SNE
    try:
        n = len(mids)
        perp = min(30, max(5, int(n / 10)))  # Heuristic
        tsne = TSNE(
            n_components=2,
            perplexity=perp,
            random_state=42,
            init="pca",
            learning_rate="auto",
        )
        X_tsne = tsne.fit_transform(X)
        plot_embedding(
            X_tsne,
            strains,
            subset_name,
            os.path.join(out_dir, "tsne_mouse.png"),
            f"t-SNE (perp={perp})",
        )
    except Exception as e:
        print(f"  t-SNE failed: {e}")


def main():
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)

    g = load_ontology()
    print("Computing transitive closure...")
    ancestors_map = get_ancestors_cache(g)

    df = pd.read_csv(DATA_FILE, sep="\t")
    df.columns = [c.strip() for c in df.columns]

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

    print(f"\nMouse-level analysis complete. Results in {OUTPUT_DIR}/")


if __name__ == "__main__":
    main()
