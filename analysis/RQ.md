# Research Question: Over-representation of Pathology and Anatomy Classes in Mice

## Question
Are specific Mouse Anatomy (MA), Mouse Pathology (MPATH), or integrated Pathology-Anatomy-Mapping (PAM/MAP) classes significantly over-represented in certain mouse strains or sexes when controlling for the age of the mouse and considering the ontology hierarchy?

## Method
1. **Data Source**: Integrated dataset derived from `Detailed Dx List.csv` and `completeDataID.csv`, mapped to MA, MPATH, and PAM/MAP IRIs.
2. **True Path Rule (TPR)**: Every mouse observation was propagated to all its super-classes in the ontology hierarchy (using `ma.owl`, `mpath.owl`, `PAM.owl`, and `map.owl`). This ensures that enrichment is captured at all levels of granularity.
3. **Control for Age**: Analysis used the categorical age bins provided in the source data (**6M, 12M, 20M, LONG**).
4. **Statistical Analysis**:
   - Hypergeometric tests were performed within each age bin for each factor (Strain, Sex, and Strain+Sex interaction).
   - P-values were combined across bins and corrected using the **Bonferroni method**.
   - **Exclusion of Trivial Findings**: For Sex and "Both" factors, all pathologies occurring in sex-specific organs (e.g., ovaries, testes) were excluded.

## Findings
The analysis with True Path Rule propagation revealed **2,326 significant enrichments** (AdjP < 0.05). The propagation allows us to see enrichment not just in specific tissues, but in broader anatomical systems.

Detailed findings are documented in **[analysis/python_summary_tpr.md](python_summary_tpr.md)**.

### Representative Significant Non-Trivial Associations (with TPR)
| Factor | Value | Class | Obs | Exp | AdjP |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Sex | F | MPATH:696 (Adrenal spindle cell hyperplasia) | 682 | 442.72 | < 1e-10 |
| Sex | F | MA:0000116 (Adrenal gland) | 694 | 451.96 | < 1e-10 |
| Sex | M | MPATH:743 (Hepatic fatty metamorphosis) | 148 | 90.89 | < 1e-10 |
| Strain | KK/HlJ | PAM_0001876x181 (Renal papilla hyaline cast) | 62 | 8.88 | < 1e-10 |
| Both | C57BL/10J_M | PAM_0001876x127 (Renal papilla atrophy) | 24 | 1.33 | < 1e-10 |

## Reproducibility
All analysis code and intermediate files are preserved in the `analysis/` folder:
- `extract_hierarchy.groovy`: Extracts transitive closure of ontologies.
- `perform_stats.py`: Python script (run via `uv`) for the final ORA with TPR.
- `enrichments_python_tpr.tsv`: Full table of significant results.
- `top_enrichments_tpr.png`: Visualization of top results.
