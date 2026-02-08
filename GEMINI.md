# Project Overview: Mouse Pathology-Anatomy Mapping (mpath-ma)

This project focuses on the integration and mapping of mouse anatomy and pathology data using ontologies. It leverages the **Adult Mouse Anatomy (MA)** and **Mouse Pathology (MPATH)** ontologies to create combined mapping ontologies like **PAM (Pathology-Anatomy-Mapping)** and **MAP (Mouse-Anatomy-Pathology)**.

The core of the project consists of:
- **Ontologies**: `ma.owl`, `mpath.owl`, `PAM.owl`, `map.owl`, and `PAMt.owl`.
- **Data**: CSV files containing mouse pathology observations (`Detailed Dx List.csv`, `completeDataID.csv`).
- **Scripts**: Groovy scripts using the **OWLAPI** and **ELK Reasoner** to process data and generate integrated ontologies.

## Main Technologies
- **Groovy**: Used for automation and data processing scripts.
- **OWLAPI (4.2.5)**: The primary library for reading, manipulating, and saving OWL ontologies.
- **ELK Reasoner**: Used for efficient reasoning over the large ontologies.
- **Grapes**: Groovy's dependency management system used within scripts to pull in OWLAPI and SLF4J.

## Project Structure
- `*.owl`: Core and generated ontologies.
- `*.csv`: Source data files.
- `generate*.groovy`: Scripts for generating different versions of the mapping ontologies.
- `script/`: A directory containing utility scripts and documentation (e.g., `generateMappings.groovy`).
- `catalog-v001.xml`: Protege/OWLAPI catalog file for managing local ontology redirects.

## Building and Running

### Prerequisites
- **Java 21+**: Ensure you have a modern JDK installed.
- **Groovy**: Installed and available in your PATH.

### Running Scripts
Most scripts are self-contained Groovy files that use `@Grapes` for dependency management. Due to changes in Java's module system (Java 9+), you must often provide the `--add-opens` flag to allow internal access required by some older libraries used in the OWLAPI toolchain.

Example command to run a script:
```bash
export JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"
groovy <script_name>.groovy --output-file <output.owl>
```

### Key Scripts
- `generateMAPnew.groovy`: Generates the MAP ontology from `completeDataID.csv`.
- `generatePAMnew.groovy`: Generates the PAM ontology.
- `generateAllCombinations_MAP.groovy`: Generates a MAP ontology by combining all classes from MA and MPATH.
- `script/generateMappings.groovy`: Generates a TSV mapping file linking mouse data to all four ontologies.

## Development Conventions

- **IRI Patterns**: 
  - MA: `http://purl.obolibrary.org/obo/MA_...`
  - MPATH: `http://purl.obolibrary.org/obo/MPATH_...`
  - PAM: `http://phenomebrowser.net/pam/PAM_...x...`
  - MAP: `http://phenomebrowser.net/map/MAP_...x...`
- **Ontology Management**: Use `catalog-v001.xml` to ensure that tools like Protege or OWLAPI use local copies of the ontologies instead of attempting to fetch them from the web.
- **Reasoning**: ELK is the preferred reasoner for these ontologies due to their size and the specific logic used (EL profile).
