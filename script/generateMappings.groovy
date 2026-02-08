@Grapes([
    @Grab(group='org.slf4j', module='slf4j-simple', version='1.6.1'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.2.5'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.2.5'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.2.5'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.2.5')
])

import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.*
import java.io.File

def manager = OWLManager.createOWLOntologyManager()
def fac = manager.getOWLDataFactory()

println "Loading ontologies..."
def maOnt = manager.loadOntologyFromOntologyDocument(new File('ma.owl'))
def mpathOnt = manager.loadOntologyFromOntologyDocument(new File('mpath.owl'))
def pamOnt = manager.loadOntologyFromOntologyDocument(new File('PAM.owl'))
def mapOnt = manager.loadOntologyFromOntologyDocument(new File('map.owl'))

def maBase = "http://purl.obolibrary.org/obo/"
def mpathBase = "http://purl.obolibrary.org/obo/"
def pamBase = "http://phenomebrowser.net/pam/"
def mapBase = "http://phenomebrowser.net/map/"

def output = new File('mouse_mappings.tsv')
output.withWriter { writer ->
    writer.write("MouseID\tStrain\tSex\tAge\tAgeBin\tMA_ID\tMPATH_ID\tMA_IRI\tMPATH_IRI\tPAM_IRI\tMAP_IRI\n")

    def processLine = { line, maCol, mpathCol, strainCol, sexCol, ageCol, binCol, yrCol, caseCol ->
        if (line.size() <= Math.max(maCol, Math.max(mpathCol, Math.max(strainCol, Math.max(sexCol, Math.max(ageCol, Math.max(binCol, Math.max(yrCol, caseCol)))))))) return

        def maRaw = line[maCol]
        def mpathRaw = line[mpathCol]
        
        def maMatcher = maRaw =~ /MA:(\d+)/
        def mpathMatcher = mpathRaw =~ /MPATH:(\d+)/
        
        if (maMatcher && mpathMatcher) {
            def maNum = maMatcher[0][1]
            def mpathNum = mpathMatcher[0][1]
            
            def mouseId = "${line[yrCol]}_${line[caseCol]}"
            def strain = line[strainCol]
            def sex = line[sexCol]
            def age = line[ageCol]
            def ageBin = line[binCol].trim().toUpperCase()
            
            def maIRI = "${maBase}MA_${maNum}"
            def mpathIRI = "${mpathBase}MPATH_${mpathNum}"
            def pamIRI = "${pamBase}PAM_${maNum}x${mpathNum}"
            def mapIRI = "${mapBase}MAP_${maNum}x${mpathNum}"
            
            writer.write("${mouseId}\t${strain}\t${sex}\t${age}\t${ageBin}\tMA:${maNum}\tMPATH:${mpathNum}\t${maIRI}\t${mpathIRI}\t${pamIRI}\t${mapIRI}\n")
        }
    }

    println "Processing Detailed Dx List.csv..."
    new File('Detailed Dx List.csv').eachLine { lineStr ->
        if (lineStr.trim().isEmpty()) return
        def line = lineStr.split('\t')
        // Detailed Dx List: 13th col (idx 12) is the bin (20M, etc)
        processLine(line, 5, 6, 2, 3, 4, 12, 0, 1)
    }

    println "Processing completeDataID.csv..."
    def first = true
    new File('completeDataID.csv').eachLine { lineStr ->
        if (first) { first = false; return }
        if (lineStr.trim().isEmpty()) return
        def line = lineStr.split(',')
        // completeDataID: 18th col (idx 17) is the bin (LONG, etc)
        processLine(line, 7, 10, 3, 4, 5, 17, 1, 2)
    }
}

println "Mappings regenerated in mouse_mappings.tsv"