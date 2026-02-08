# Installation and Running Instructions (MacOS)

This guide provides step-by-step instructions to set up and run the Mouse Pathology-Anatomy Mapping project on a MacOS laptop.

## 1. Prerequisites (Homebrew)

If you don't have Homebrew installed, open your terminal and run:
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

## 2. Install Development Tools

### Java and Groovy
The mapping and hierarchy extraction scripts require Java and Groovy.
```bash
brew install openjdk groovy
```
*Note: You may need to follow the post-install instructions from `brew` to symlink the openjdk.*

### Python and uv
The statistical analysis and visualizations require Python, managed by `uv` for easy dependency handling.
```bash
brew install uv
```

### Gemini CLI
For interactive AI assistance in the terminal:
```bash
npm install -g @google/gemini-cli
```

## 3. Running the Project

Follow these steps in order within the project root directory:

### Step 1: Generate Mappings
Generate the `mouse_mappings.tsv` file from the raw CSV data.
```bash
export JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"
groovy script/generateMappings.groovy
```

### Step 2: Extract Hierarchy
Extract the transitive closure of the ontology hierarchies.
```bash
export JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"
groovy analysis/extract_hierarchy.groovy
```

### Step 3: Run Statistical Analysis
Perform the over-representation analysis (ORA) with True Path Rule and generate plots.
```bash
uv run --with pandas --with scipy --with matplotlib --with tabulate analysis/perform_stats.py
```

## 4. Viewing Results
- **Summary**: Check `analysis/RQ.md` and `analysis/stats_summary.md`.
- **Full Data**: See `analysis/enrichments_python_tpr.tsv`.
- **Plots**: View `analysis/top_enrichments_tpr.png`.
