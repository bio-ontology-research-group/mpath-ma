# Statistical Enrichment Summary (Refined)

This document summarizes the significant over-representation of anatomy and pathology classes across mouse strains, sexes, and their combinations, controlled for age. **Trivial sex-specific associations (e.g., pathologies in ovaries, testes, etc.) have been excluded from the sex-based analysis.**

## Methodology
- **Test**: Hypergeometric test performed within each age bin (Young, Middle, Old).
- **Control for Age**: Fisher-like combination of p-values across bins (minimum p-value with bin-level Bonferroni correction).
- **Multiple Testing Correction**: Bonferroni correction across all factor-class combinations tested.
- **Significance Threshold**: Adjusted P-value (AdjP) < 0.05.
- **Exclusion Criteria**: For Factor "Sex" and "Both", any classes involving sex-specific organs (Ovary, Testis, Uterus, Prostate, Seminal Vesicle, Epididymis, etc.) were excluded.

## Overall Statistics
- **Total Tests Performed**: 3,118
- **Significant Enrichments**: 1,584 (after excluding trivial sex findings)

## Top Significant Non-Trivial Findings

### By Sex (Non-Trivial)
These are pathologies occurring in organs shared by both sexes but showing significant sex-based enrichment.

| Sex | Class | Description | Obs | Exp | AdjP |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F | PAM_0000116x696 | Adrenal gland has lesion spindle cell hyperplasia | 682 | 442.72 | < 1e-10 |
| M | PAM_0000358x743 | Liver has lesion hepatic fatty metamorphosis | 148 | 90.89 | < 1e-10 |
| F | MPATH:43 | Lipofuscin deposition | 356 | 247.59 | < 1e-10 |
| F | MPATH:590 | Fibro-osseous lesion | 266 | 161.65 | < 1e-10 |
| F | PAM_0001459x590 | Bone has lesion fibro-osseous lesion | 260 | 158.30 | < 1e-10 |

- **Females** show a massive enrichment for **adrenal spindle cell hyperplasia** and **fibro-osseous lesions** in bones.
- **Males** are significantly more likely to exhibit **hepatic fatty metamorphosis**.

### By Strain
| Strain | Class | Description (MA x MPATH) | Obs | Exp | AdjP |
| :--- | :--- | :--- | :--- | :--- | :--- |
| KK/HlJ | PAM_0001876x181 | Renal papilla has lesion hyaline cast | 62 | 8.88 | < 1e-10 |
| MRL/MpJ | MA:0000120 | Heart | 57 | 12.33 | < 1e-10 |
| SJL/J | PAM_0000276x14 | Mesenteric lymph node has lesion hyperplasia | 28 | 4.20 | < 1e-10 |

### By Strain & Sex (Interaction, Non-Trivial)
| Combination | Class | Description | Obs | Exp | AdjP |
| :--- | :--- | :--- | :--- | :--- | :--- |
| C57BL/10J_M | PAM_0001876x127 | Renal papilla has lesion atrophy | 24 | 1.33 | < 1e-10 |

## Discussion
After excluding trivial findings (like ovarian cysts in females), we observe profound biological differences in disease susceptibility between sexes. The enrichment of **adrenal spindle cell hyperplasia** in females is particularly striking. Strain remains a dominant factor for many specific organ-pathology combinations.

Complete results are available in `analysis/enrichments_stats.tsv`.