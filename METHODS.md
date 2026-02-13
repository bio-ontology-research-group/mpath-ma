# Materials and Methods

This document details the data sources, ontology construction, and statistical methodologies used to analyze mouse pathology data across 31 inbred strains.

## 1. Study Population and Data Preprocessing

### Data Source
The primary dataset consists of necropsy and pathology records for 1,713 mice across 31 inbred strains. The raw data was processed to sanitize identifiers (removing formatting artifacts) and standardize ontology references.

### Cohort Demographics
The dataset is stratified by **Sex** and **Age Cohort (Code)**.
*   **Total Animals**: N = 1,713
*   **Sex Distribution**: Female (N=902), Male (N=811).
*   **Age Cohorts**:
    *   **6M**: 6 months (N=73)
    *   **12M**: 12 months (N=764)
    *   **20M**: 20 months (N=552)
    *   **LONG**: Longitudinal / End of Life (N=324)

### Phenotype Annotation
Each observation (lesion) was mapped to three ontological spaces:
1.  **MA (Mouse Anatomy)**: The organ or tissue affected.
2.  **MPATH (Mouse Pathology)**: The specific pathological diagnosis.
3.  **PAMT (Pathology-Anatomy Mouse Terminology)**: A pre-coordinated ontology combining the two (see below).

## 2. Ontology Construction

### PAMT (Pathology-Anatomy Mouse Terminology)
To capture the specific context of lesions (e.g., "Carcinoma *of the* Liver"), we constructed the PAMT ontology (`PAMTv2.owl`). PAMT is defined as a cross-product of MPATH and MA using the Web Ontology Language (OWL).

*   **Structure**: PAMT classes are defined as intersections of a pathology term and an anatomical location.
    *   *Logical Definition*: `PAMT_Class ≡ MPATH_Term ⊓ ∃ affects.MA_Term`
*   **Hierarchy**: The ontology hierarchy is inferred using reasoners (ELK) or graph-based transitive closure, ensuring that "Liver Carcinoma" is correctly classified as both a "Liver Neoplasm" and a "Carcinoma".
*   **Root**: The analysis uses `MPATH:0` (Pathological Entity) as the root node for all pathology-based analyses.

## 3. Ontology Enrichment Analysis

We performed Over-Representation Analysis (ORA) to identify lesions significantly associated with specific strains.

### Algorithm
We utilized the `func` (Functional Analysis) software package (v0.4.10), which implements a **Hypergeometric Test** to assess the probability of observing $k$ or more hits in a category given the population size $N$, category size $K$, and sample size $n$.

### Stratification and Controls
To control for confounding factors (Sex and Age), we implemented a **Stratum-Restricted Merged Background** strategy:
1.  For a target strain $S$, we identified all unique tuples of $(Sex, Code)$ present in $S$.
2.  The background set $B$ was constructed by pooling all animals from the remaining 30 strains that matched these specific $(Sex, Code)$ tuples.
3.  This ensures that, for example, a strain with only female data is compared exclusively against a female background population, preventing sex-biased lesions from appearing as strain-specific artifacts.

### Topology Refinement
Standard enrichment analysis on DAGs (Directed Acyclic Graphs) suffers from redundancy (e.g., if "Lung Tumor" is significant, the parent term "Tumor" is often trivially significant). We applied `func`'s **Refinement Algorithm**, which:
*   Assess whether a parent term's significance is solely due to the signal from its significant children.
*   Removes parent terms that do not provide additional information (no unique significant hits outside the child terms).
*   Reports **Refined P-values**.

### Statistical Correction
*   **P-values**: Calculated for both Over-representation and Under-representation.
*   **FDR**: Benjamini-Hochberg False Discovery Rate (FDR) was calculated on the *Refined P-values* to control for multiple testing within each analysis group.
*   **Filtering**: Results were filtered for:
    *   Refined P-value < 0.05
    *   Minimum number of hits $\ge 3$ per group.

## 4. Semantic Similarity and Clustering

To quantify the phenotypic similarity between strains, we computed **Weighted Semantic Similarity** profiles.

### Vector Representation (True Path Rule)
Strains were represented as high-dimensional vectors in the ontology space.
1.  **Propagation**: We applied the **True Path Rule (TPR)**. If a strain exhibits a specific lesion (e.g., *Adenoma of Harderian Gland*), the count is propagated up the ontology graph to all ancestors (e.g., *Adenoma*, *Neoplasm*, *Pathological Entity*).
2.  **Normalization**: Counts were normalized by the total number of animals in the strain to represent **Lesion Prevalence** (0.0 to 1.0).

### Similarity Metric
We computed the **Cosine Similarity** between the prevalence vectors of every pair of strains. This metric captures the alignment of disease profiles while being robust to differences in the absolute magnitude of lesion counts (though normalization already addresses this).

### Clustering and Visualization
*   **Clustering**: Hierarchical clustering was performed using the **UPGMA** (Average Linkage) method on the Euclidean distances derived from the similarity vectors.
*   **Dimensionality Reduction**:
    *   **Strain-Level**: Principal Component Analysis (PCA) on the prevalence vectors.
    *   **Mouse-Level**: PCA and t-SNE (t-Distributed Stochastic Neighbor Embedding) on binary TPR-propagated vectors for individual animals to visualize intra-strain variability.

## 5. Software Environment

The analysis pipeline was implemented in Python (3.10+) and Groovy. Key dependencies include:
*   **Data Processing**: `pandas`, `numpy`
*   **Ontology Manipulation**: `networkx`, `rdflib`, OWLAPI (via Groovy)
*   **Statistics**: `scipy`, `func` (C++ binary)
*   **Machine Learning**: `scikit-learn` (PCA, t-SNE, Cosine Similarity)
*   **Visualization**: `matplotlib`, `seaborn`

All scripts are version-controlled and designed for reproducibility, utilizing `uv` for dependency management.
