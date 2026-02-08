@Grapes([
    @Grab(group='org.slf4j', module='slf4j-simple', version='1.6.1'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.2.5'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.2.5'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.2.5'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.2.5')
])

import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory
import java.io.File

def manager = OWLManager.createOWLOntologyManager()
def fac = manager.getOWLDataFactory()
def rf = new StructuralReasonerFactory()

def extractHierarchy = { file ->
    println "Processing hierarchy for ${file}..."
    def ont = manager.loadOntologyFromOntologyDocument(new File(file))
    def reasoner = rf.createReasoner(ont)
    
    def mapping = [:]
    ont.getClassesInSignature().each { cls ->
        def iri = cls.getIRI().toString()
        def id = iri.contains('_') ? iri.split('_').last() : iri.split('/').last()
        if (iri.contains("MA_")) id = "MA:" + id
        if (iri.contains("MPATH_")) id = "MPATH:" + id
        
        def ancestors = reasoner.getSuperClasses(cls, false).getFlattened()
        def ancestorIds = ancestors.collect { aCls ->
            def aIri = aCls.getIRI().toString()
            def aId = aIri.contains('_') ? aIri.split('_').last() : aIri.split('/').last()
            if (aIri.contains("MA_")) aId = "MA:" + aId
            if (aIri.contains("MPATH_")) aId = "MPATH:" + aId
            return aId
        }.findAll { it != "Nothing" && it != "Thing" }
        
        mapping[id] = ancestorIds
    }
    manager.removeOntology(ont)
    return mapping
}

def fullHierarchy = [:]
fullHierarchy.putAll(extractHierarchy('ma.owl'))
fullHierarchy.putAll(extractHierarchy('mpath.owl'))
fullHierarchy.putAll(extractHierarchy('PAM.owl'))
fullHierarchy.putAll(extractHierarchy('map.owl'))

def out = new File('analysis/hierarchy.tsv')
out.withWriter { w ->
    fullHierarchy.each { child, parents ->
        if (parents) {
            w.write(child + "\t" + parents.join(",") + "\n")
        }
    }
}
println "Hierarchy extracted to analysis/hierarchy.tsv"