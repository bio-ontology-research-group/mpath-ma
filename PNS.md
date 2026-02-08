# Installation and Running Instructions (MacOS)

This guide provides the fastest way to set up and run the Mouse Pathology-Anatomy Mapping project on a MacOS laptop using AI assistance.

## 1. Quick Start (AI-Assisted Setup)

If you have `gemini-cli` installed, you can let the AI handle the entire installation and configuration process for you.

1. **Install gemini-cli** (if not already present):
   ```bash
   npm install -g @google/gemini-cli
   ```

2. **Run the following prompt**:
   Open the project folder and run:
   ```bash
   gemini "Set up my MacOS development environment for this project. Install Homebrew (if missing), Java, Groovy, and uv, then run the full analysis pipeline."
   ```

---

## 2. Manual Installation Steps

If you prefer to install the tools yourself, follow these steps:

### Prerequisites (Homebrew)
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### Install Development Tools
```bash
brew install openjdk groovy uv
```

## 3. Running the Analysis Pipeline

Once the tools are installed, execute the following commands in order:

1. **Generate Mappings**:
   ```bash
   export JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"
   groovy script/generateMappings.groovy
   ```

2. **Extract Hierarchy**:
   ```bash
   export JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"
   groovy analysis/extract_hierarchy.groovy
   ```

3. **Run Statistical Analysis**:
   ```bash
   uv run --with pandas --with scipy --with matplotlib --with tabulate analysis/perform_stats.py
   ```

## 4. Viewing Results
- **Summary**: `analysis/RQ.md` and `analysis/stats_summary.md`
- **Full Data**: `analysis/enrichments_python_tpr.tsv`
- **Plots**: `analysis/top_enrichments_tpr.png`