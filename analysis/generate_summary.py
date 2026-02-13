import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import os
import numpy as np

# Configuration
DATA_FILE = "data/cleaned_with_pamt.tsv"
BASE_OUTPUT_DIR = "analysis"

# Setup Plotting Style
sns.set_theme(style="whitegrid")
plt.rcParams["figure.figsize"] = (12, 8)


def load_data():
    print("Loading data...")
    df = pd.read_csv(DATA_FILE, sep="\t")
    df.columns = [c.strip() for c in df.columns]
    return df


def generate_summary_stats(df, output_dir, title_prefix):
    print(f"[{title_prefix}] Generating summary stats...")

    n_animals = df["new IDs"].nunique()
    n_strains = df["Strain"].nunique()
    n_ma = df["Organ MA code"].nunique()
    n_mpath = df["Diagnoses MPATH code"].nunique()
    n_pamt = df["PAMT_IRI"].nunique()

    stats_md = f"""# Dataset Summary: {title_prefix}

- **Total Observations (Rows)**: {len(df)}
- **Total Unique Animals**: {n_animals}
- **Number of Strains**: {n_strains}
- **Ontology Coverage**:
    - **MA Terms (Anatomy)**: {n_ma} unique terms
    - **MPATH Terms (Pathology)**: {n_mpath} unique terms
    - **PAMT Terms (Combined)**: {n_pamt} unique terms

## Distributions
"""
    # Sex Distribution
    if "Sex" in df.columns and df["Sex"].nunique() > 1:
        sex_counts = df.groupby("Sex")["new IDs"].nunique()
        stats_md += "\n### Animals by Sex\n" + sex_counts.to_markdown() + "\n"

    # Code Distribution
    if "Code" in df.columns and df["Code"].nunique() > 1:
        code_counts = df.groupby("Code")["new IDs"].nunique()
        stats_md += "\n### Animals by Code\n" + code_counts.to_markdown() + "\n"

    # Strain Counts (Top 10)
    strain_counts = (
        df.groupby("Strain")["new IDs"].nunique().sort_values(ascending=False).head(10)
    )
    stats_md += (
        "\n### Top 10 Strains by Animal Count\n" + strain_counts.to_markdown() + "\n"
    )

    with open(os.path.join(output_dir, "summary_stats.md"), "w") as f:
        f.write(stats_md)


def plot_strain_demographics(df, output_dir, title_prefix):
    print(f"[{title_prefix}] Plotting demographics...")
    animal_meta = df[["new IDs", "Strain", "Sex", "Code"]].drop_duplicates()

    if len(animal_meta) == 0:
        return

    # Strain vs Sex (Stacked)
    # Even if filtering by Sex (e.g. only F), this shows the count per strain clearly.
    plt.figure(figsize=(14, 8))
    strain_sex = animal_meta.groupby(["Strain", "Sex"]).size().unstack(fill_value=0)

    if strain_sex.empty:
        return

    # Sort by total count
    strain_sex["Total"] = strain_sex.sum(axis=1)
    strain_sex = strain_sex.sort_values("Total", ascending=False).drop("Total", axis=1)

    strain_sex.plot(
        kind="bar", stacked=True, width=0.8, colormap="viridis", figsize=(14, 8)
    )
    plt.title(f"{title_prefix}: Animal Count per Strain by Sex")
    plt.ylabel("Number of Animals")
    plt.xlabel("Strain")
    plt.xticks(rotation=45, ha="right")
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, "demographics_strain_sex.png"))
    plt.close()


def plot_ontology_landscape(df, output_dir, title_prefix):
    print(f"[{title_prefix}] Plotting ontology landscape...")
    # Top MA vs MPATH
    top_n = 25

    if len(df) < 50:
        print("  Not enough data for landscape.")
        return

    top_ma = df["Organ Name"].value_counts().head(top_n).index
    top_mpath = df["Diagnosis Name"].value_counts().head(top_n).index

    subset = df[df["Organ Name"].isin(top_ma) & df["Diagnosis Name"].isin(top_mpath)]

    if subset.empty:
        return

    heatmap_data = (
        subset.groupby(["Organ Name", "Diagnosis Name"]).size().unstack(fill_value=0)
    )

    plt.figure(figsize=(16, 14))
    sns.heatmap(heatmap_data, cmap="YlOrRd", annot=False, linewidths=0.5)
    plt.title(f"{title_prefix}: Top {top_n} Anatomy vs Pathology Interaction")
    plt.xlabel("Pathology (MPATH)")
    plt.ylabel("Anatomy (MA)")
    plt.xticks(rotation=45, ha="right")
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, "ontology_landscape.png"))
    plt.close()


def plot_strain_clustering(df, output_dir, title_prefix):
    print(f"[{title_prefix}] Plotting strain clustering...")

    if df["Strain"].nunique() < 2:
        print("  Not enough strains for clustering.")
        return

    strain_total = df.groupby("Strain")["new IDs"].nunique()

    # Take top PAMT terms (e.g. top 40)
    top_pamt = df["PAMT_Label"].value_counts().head(40).index
    subset = df[df["PAMT_Label"].isin(top_pamt)]

    if subset.empty:
        return

    strain_term_counts = subset.groupby(["Strain", "PAMT_Label"])["new IDs"].nunique()
    matrix = strain_term_counts.unstack(fill_value=0)

    # Normalize
    common_strains = matrix.index.intersection(strain_total.index)
    matrix = matrix.loc[common_strains]
    totals = strain_total.loc[common_strains]

    # Avoid division by zero
    totals = totals[totals > 0]
    matrix = matrix.loc[totals.index]

    prevalence = matrix.div(totals, axis=0) * 100
    prevalence = prevalence.fillna(0)

    # Filter out empty rows/cols
    prevalence = prevalence.loc[:, (prevalence != 0).any(axis=0)]

    if prevalence.shape[0] < 2 or prevalence.shape[1] < 2:
        print("  Matrix too small for clustering.")
        return

    try:
        g = sns.clustermap(
            prevalence,
            cmap="viridis",
            figsize=(16, 14),
            linewidths=0.5,
            dendrogram_ratio=(0.1, 0.2),
            cbar_pos=(0.02, 0.8, 0.03, 0.15),
        )

        g.ax_heatmap.set_title(
            f"{title_prefix}: Strain Similarity by Lesion Prevalence (%)", pad=100
        )
        g.ax_heatmap.set_xlabel("PAMT Term (Lesion)")
        g.ax_heatmap.set_ylabel("Strain")

        plt.setp(g.ax_heatmap.get_xticklabels(), rotation=45, ha="right")
        plt.setp(g.ax_heatmap.get_yticklabels(), rotation=0)

        g.savefig(os.path.join(output_dir, "strain_clustering.png"))
        plt.close()
    except Exception as e:
        print(f"  Clustering failed (likely data sparsity): {e}")


def run_suite(df, output_subdir, title_prefix):
    output_dir = os.path.join(BASE_OUTPUT_DIR, output_subdir)
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    generate_summary_stats(df, output_dir, title_prefix)
    plot_strain_demographics(df, output_dir, title_prefix)
    plot_ontology_landscape(df, output_dir, title_prefix)
    plot_strain_clustering(df, output_dir, title_prefix)


def main():
    if not os.path.exists(BASE_OUTPUT_DIR):
        os.makedirs(BASE_OUTPUT_DIR)

    df = load_data()

    # 1. Global Analysis
    run_suite(df, "Global", "All Data")

    # 2. Split by Sex
    sexes = df["Sex"].dropna().unique()
    for sex in sexes:
        print(f"\n--- Analyzing Sex: {sex} ---")
        sub_df = df[df["Sex"] == sex]
        run_suite(sub_df, f"Sex_{sex}", f"Sex: {sex}")

    # 3. Split by Code (Timepoint)
    codes = df["Code"].dropna().unique()
    for code in codes:
        print(f"\n--- Analyzing Code: {code} ---")
        sub_df = df[df["Code"] == code]
        run_suite(sub_df, f"Code_{code}", f"Code: {code}")

    print(f"\nAll analyses complete. Results in {BASE_OUTPUT_DIR}/")


if __name__ == "__main__":
    main()
