# Mouse Mapping Script

This script generates a TSV mapping file that links mouse data from CSV files to four ontologies:
1. Mouse Anatomy (MA)
2. Mouse Pathology (MPATH)
3. PAM (Pathology-Anatomy-Mapping)
4. MAP (Mouse-Anatomy-Pathology)

## Dependencies

- Groovy
- OWLAPI 4.2.5 (handled via @Grapes)
- Java 9+ requires `--add-opens java.base/java.lang=ALL-UNNAMED`

## Usage

Run the following command in the root directory:

```bash
export JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"
groovy script/generateMappings.groovy
```

The output will be saved to `mouse_mappings.tsv`.

## Mapping Logic

- **MouseID**: Combination of `NECROP_YR` and `Case number`.
- **MA_IRI**: `http://purl.obolibrary.org/obo/MA_<ID>`
- **MPATH_IRI**: `http://purl.obolibrary.org/obo/MPATH_<ID>`
- **PAM_IRI**: `http://phenomebrowser.net/pam/PAM_<MA_ID>x<MPATH_ID>`
- **MAP_IRI**: `http://phenomebrowser.net/map/MAP_<MA_ID>x<MPATH_ID>`
