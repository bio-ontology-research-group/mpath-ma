import pandas as pd
import numpy as np
from scipy.stats import hypergeom
import matplotlib.pyplot as plt
import os

# Sex-specific MA IDs to be excluded from sex-based analysis
SEX_SPECIFIC_MA = {
    'MA:0000384', 'MA:0000411', 'MA:0000389', 'MA:0000410', 
    'MA:0000397', 'MA:0000404', 'MA:0001702', 'MA:0000403', 
    'MA:0000405', 'MA:0000385', 'MA:0000412'
}

def is_trivial(factor, cls):
    if factor in ["Sex", "Both"]:
        if cls in SEX_SPECIFIC_MA:
            return True
        if cls.startswith("PAM_") or cls.startswith("MAP_"):
            # Check if MA part is sex-specific
            # Format can be PAM_000...x000...
            parts = cls.split('_')[-1].split('x')
            if len(parts) == 2:
                ma_id = "MA:" + parts[0]
                if ma_id in SEX_SPECIFIC_MA:
                    return True
    return False

# Load hierarchy for True Path Rule
hierarchy = {}
if os.path.exists('analysis/hierarchy.tsv'):
    with open('analysis/hierarchy.tsv', 'r') as f:
        for line in f:
            parts = line.strip().split('\t')
            if len(parts) == 2:
                hierarchy[parts[0]] = parts[1].split(',')

# Load data
df = pd.read_csv('mouse_mappings.tsv', sep='\t')

# Unique mice data
mice = df.groupby('MouseID').first()[['Strain', 'Sex', 'AgeBin']]
age_bins = [b for b in mice['AgeBin'].unique() if pd.notna(b)]

# Class occurrences per mouse with True Path Rule propagation
mouse_classes = {}
for mid, group in df.groupby('MouseID'):
    classes = set(group['MA_ID']) | set(group['MPATH_ID'])
    classes |= {iri.split('/')[-1] for iri in group['PAM_IRI']}
    classes |= {iri.split('/')[-1] for iri in group['MAP_IRI']}
    
    # Propagate through hierarchy
    expanded = set(classes)
    for cls in classes:
        if cls in hierarchy:
            expanded.update(hierarchy[cls])
    mouse_classes[mid] = expanded

all_classes = sorted(list(set().union(*mouse_classes.values())))
all_strains = mice['Strain'].unique()
all_sexes = mice['Sex'].unique()
all_both = (mice['Strain'] + "_" + mice['Sex']).unique()

def run_ora(factor_name, factor_values):
    results = []
    total_mice_per_bin = mice.groupby('AgeBin').size().to_dict()
    
    class_counts_per_bin = {}
    for ab in age_bins:
        bin_mice = mice[mice['AgeBin'] == ab].index
        counts = {}
        for mid in bin_mice:
            for cls in mouse_classes.get(mid, []):
                counts[cls] = counts.get(cls, 0) + 1
        class_counts_per_bin[ab] = counts

    if factor_name == "Strain":
        mice_groups = mice.groupby(['Strain', 'AgeBin']).size().to_dict()
    elif factor_name == "Sex":
        mice_groups = mice.groupby(['Sex', 'AgeBin']).size().to_dict()
    else: # Both
        mice_groups = (mice['Strain'] + "_" + mice['Sex']).to_frame('Val').join(mice['AgeBin']).groupby(['Val', 'AgeBin']).size().to_dict()

    group_class_counts = {}
    for mid, m_classes in mouse_classes.items():
        m_data = mice.loc[mid]
        if pd.isna(m_data['AgeBin']): continue
        
        if factor_name == "Strain": val = m_data['Strain']
        elif factor_name == "Sex": val = m_data['Sex']
        else: val = m_data['Strain'] + "_" + m_data['Sex']
        
        ab = m_data['AgeBin']
        for cls in m_classes:
            key = (cls, val, ab)
            group_class_counts[key] = group_class_counts.get(key, 0) + 1

    for val in factor_values:
        for cls in all_classes:
            if is_trivial(factor_name, cls):
                continue
            
            total_observed = 0
            total_expected = 0
            p_values = []
            
            for ab in age_bins:
                n_group_bin = mice_groups.get((val, ab), 0)
                if n_group_bin == 0: continue
                
                N_bin = total_mice_per_bin[ab]
                C_bin = class_counts_per_bin[ab].get(cls, 0)
                k_observed = group_class_counts.get((cls, val, ab), 0)
                
                if N_bin > 0:
                    p = hypergeom.sf(k_observed - 1, N_bin, C_bin, n_group_bin)
                    p_values.append(p)
                    total_observed += k_observed
                    total_expected += n_group_bin * C_bin / N_bin
            
            if total_observed >= 10 and p_values: # Raised min count for TPR robustness
                combined_p = min(p_values) * len(p_values)
                combined_p = min(combined_p, 1.0)
                
                if combined_p < 0.05:
                    results.append({
                        'Factor': factor_name,
                        'Value': val,
                        'Class': cls,
                        'Observed': total_observed,
                        'Expected': total_expected,
                        'Ratio': total_observed / total_expected if total_expected > 0 else 0,
                        'P-value': combined_p
                    })
    return results

print("Analyzing Strains with True Path Rule...")
results_strain = run_ora("Strain", all_strains)
print("Analyzing Sex with True Path Rule...")
results_sex = run_ora("Sex", all_sexes)
print("Analyzing Both with True Path Rule...")
results_both = run_ora("Both", all_both)

all_results = results_strain + results_sex + results_both
if not all_results:
    print("No results found.")
    exit()

res_df = pd.DataFrame(all_results)
res_df['AdjP'] = (res_df['P-value'] * len(res_df)).clip(upper=1.0)
res_df = res_df[res_df['AdjP'] < 0.05].sort_values('AdjP')

res_df.to_csv('analysis/enrichments_python_tpr.tsv', sep='\t', index=False)
print(f"Found {len(res_df)} significant enrichments (with TPR).")

# Plot Top 10
if not res_df.empty:
    top10 = res_df.head(10)
    plt.figure(figsize=(12, 6))
    labels = top10['Value'] + " | " + top10['Class']
    plt.barh(labels[::-1], top10['Ratio'][::-1], color='lightcoral')
    plt.xlabel('Enrichment Ratio (Observed/Expected)')
    plt.title('Top 10 Significant Enrichments (True Path Rule + Categorical Age)')
    plt.tight_layout()
    plt.savefig('analysis/top_enrichments_tpr.png')

# Summary for RQ.md
if not res_df.empty:
    summary_str = res_df.head(20).to_markdown(index=False)
    with open('analysis/python_summary_tpr.md', 'w') as f:
        f.write("# Python Statistical Analysis (True Path Rule)\n\n")
        f.write("Information propagated through super-classes.\n\n")
        f.write(summary_str)
        f.write("\n")
