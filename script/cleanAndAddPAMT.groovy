@Grapes([
    @Grab(group='com.opencsv', module='opencsv', version='5.7.1')
])

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.FileReader
import java.io.FileWriter

def inputFile = new File("data/Complete data with discs cleaned.csv")
def outputFile = new File("data/cleaned_with_pamt.tsv")

def pamBase = "http://phenomebrowser.net/pam/"

// Create TSV writer (tab separated, no quote character for simplicity unless needed)
def writer = new CSVWriter(new FileWriter(outputFile), '\t' as char, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)

new FileReader(inputFile).withCloseable { fr ->
    def reader = new CSVReader(fr)
    String[] header = reader.readNext()
    
    // Add PAMT_IRI and PAMT_Label to header
    def newHeader = new String[header.length + 2]
    System.arraycopy(header, 0, newHeader, 0, header.length)
    newHeader[header.length] = "PAMT_IRI"
    newHeader[header.length + 1] = "PAMT_Label"
    writer.writeNext(newHeader)

    String[] line
    while ((line = reader.readNext()) != null) {
        // Sanitize MA ID (col 7) and MPATH ID (col 10)
        // Col indices: 0-based. 
        // Header: new IDs,NECROP_YR,Case number,Strain,Sex,AgeInDays,Organ Name,Organ MA code,Organ or Tissue,Diagnosis Name,Diagnoses MPATH code,Diagnosis,Disease name,Score,Submission date,# Animals,Other ID,Code,Date Born
        
        if (line.length > 10) {
            def maRaw = line[7]
            def mpathRaw = line[10]
            
            // Clean brackets
            def maClean = maRaw.replaceAll(/\[|\]/, "")
            def mpathClean = mpathRaw.replaceAll(/\[|\]/, "")
            
            // Update line in place
            line[7] = maClean
            line[10] = mpathClean
            
            // Also clean col 8 and 11 if they contain the bracketed ID?
            // "heart interventricular septum [MA:0000085]" -> "heart interventricular septum MA:0000085"
            // The user asked to "clean the bracket issues", usually implying the IDs. 
            // Cleaning the text fields (8 and 11) is good practice too to match the IDs.
            line[8] = line[8].replaceAll(/\[|\]/, "")
            line[11] = line[11].replaceAll(/\[|\]/, "")

            // Generate PAMT IRI
            // Format: http://phenomebrowser.net/pam/PAM_maIDxmpID
            // IDs should use underscores instead of colons
            
            if (maClean.contains("MA:") && mpathClean.contains("MPATH:")) {
                def maSuffix = maClean.replaceAll("MA:", "MA_")
                def mpathSuffix = mpathClean.replaceAll("MPATH:", "MPATH_") // MPATH:123 -> MPATH_123
                
                def maNum = maClean.replaceAll("MA:", "").trim()
                def mpathNum = mpathClean.replaceAll("MPATH:", "").trim()
                
                def pamtIRI = "${pamBase}PAM_${maNum}x${mpathNum}"
                
                // Generate Label
                def organ = line[6]?.trim()
                if (!organ) {
                    organ = line[8].replaceAll(/\[?MA:[0-9]+\]?/, "").trim()
                }
                def diagnosis = line[9]?.trim()
                if (!diagnosis) {
                    diagnosis = line[11].replaceAll(/\[?MPATH:[0-9]+\]?/, "").trim()
                }
                
                def pamtLabel = "${diagnosis} affects ${organ}"

                // Add to line
                def newLine = new String[line.length + 2]
                System.arraycopy(line, 0, newLine, 0, line.length)
                newLine[line.length] = pamtIRI
                newLine[line.length + 1] = pamtLabel
                writer.writeNext(newLine)
            } else {
                // If IDs are missing or invalid, write line without PAMT (or empty)
                def newLine = new String[line.length + 2]
                System.arraycopy(line, 0, newLine, 0, line.length)
                newLine[line.length] = ""
                newLine[line.length + 1] = ""
                writer.writeNext(newLine)
            }
        } else {
             writer.writeNext(line)
        }
    }
}

writer.close()
println "Generated data/cleaned_with_pamt.tsv"
