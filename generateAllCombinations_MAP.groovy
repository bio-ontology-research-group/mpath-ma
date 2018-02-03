@Grapes([
	  @Grab(group='org.slf4j', module='slf4j-simple', version='1.6.1'),
          @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.2.5'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.2.5'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.2.5'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.2.5')
        ])

import org.semanticweb.owlapi.model.parameters.*
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration
import org.semanticweb.elk.reasoner.config.*
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.reasoner.*
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.owllink.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.search.*;

import org.semanticweb.owlapi.manchestersyntax.renderer.*;
import org.semanticweb.owlapi.reasoner.structural.*
import java.io.File;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

OWLOntologyManager manager = OWLManager.createOWLOntologyManager()
OWLDataFactory fac = manager.getOWLDataFactory()

ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor()
OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor)
ElkReasonerFactory f = new ElkReasonerFactory()

def maOnt = manager.loadOntologyFromOntologyDocument(new File('/home/sarah/groovyWorkSpace/ontology_generating/MA.owl'))
OWLReasoner maReasoner = f.createReasoner(maOnt,config)

def mpathOnt = manager.loadOntologyFromOntologyDocument(new File('/home/sarah/groovyWorkSpace/ontology_generating/MPATH.owl'))
OWLReasoner mpathReasoner = f.createReasoner(mpathOnt,config)

IRI mpathMaOntI= IRI.create('http://phenomebrowser.net/map')

Set<OWLOntology> onts  = [maOnt,mpathOnt]
OWLOntology mpathMaOnt = manager.createOntology(mpathMaOntI,onts)

def ma_part_of = fac.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/ma#part_of"))
def mpath_part_of = fac.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/mpath#part_of"))

manager.addAxiom(mpathMaOnt,fac.getOWLEquivalentObjectPropertiesAxiom(ma_part_of,mpath_part_of))
manager.addAxiom(mpathMaOnt,fac.getOWLTransitiveObjectPropertyAxiom(mpath_part_of))
manager.addAxiom(mpathMaOnt,fac.getOWLTransitiveObjectPropertyAxiom(ma_part_of))
manager.addAxiom(mpathMaOnt,fac.getOWLReflexiveObjectPropertyAxiom(mpath_part_of))
manager.addAxiom(mpathMaOnt,fac.getOWLReflexiveObjectPropertyAxiom(ma_part_of))


def L = fac.getOWLClass(IRI.create("http://phenomebrowser.net/map/MAP_0000001x0"))
mpath_0 = fac.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/MPATH_0"))
ma_0000001 = fac.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/MA_0000001"))
def has_lesion = fac.getOWLObjectProperty(IRI.create("http://phenomebrowser.net/map/has_lesion"))
OWLAnnotation label = fac.getOWLAnnotation(fac.getRDFSLabel(),fac.getOWLLiteral("MA has lesion MPATH"));
OWLAxiom axiom = fac.getOWLAnnotationAssertionAxiom(L.getIRI(), label)
manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

OWLAnnotation id = fac.getOWLAnnotation(fac.getRDFSIsDefinedBy(),fac.getOWLLiteral("MAP:0000001x0"));
fac.getOWLEquivalentClassesAxiom(L, fac.getOWLObjectIntersectionOf(fac.getOWLObjectSomeValuesFrom(has_lesion, mpath_0),ma_0000001))
axiom = fac.getOWLAnnotationAssertionAxiom(L.getIRI(), id)
manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

manager.addAxiom(mpathMaOnt, fac.getOWLEquivalentClassesAxiom(fac.getOWLThing(),fac.getOWLObjectSomeValuesFrom(has_lesion, fac.getOWLThing())))

def MAset = maOnt.getSignature();
def MPATHset = mpathOnt.getSignature();
def maclass
def mpathclass
def malabel
def macode
def mpathlabel
def mpathcode
MAset.each{
	maclass = it
	
	if (maclass instanceof OWLClass){
		macode = maclass.getIRI().toString().split('_')[1]
		//get label for maclss
		for(OWLAnnotation a : EntitySearcher.getAnnotations(maclass, maOnt, fac.getRDFSLabel())) {
		    OWLAnnotationValue val = a.getValue();
		    if(val instanceof OWLLiteral) {
		      	malabel = ((OWLLiteral) val).getLiteral()
		    }
		}

		MPATHset.each{
			mpathclass = it
			if (mpathclass instanceof OWLClass){
				mpathcode = mpathclass.getIRI().toString().split('_')[1]
				//println(macode+" "+mpathcode)
				//get label for maclss
				for(OWLAnnotation a : EntitySearcher.getAnnotations(mpathclass, mpathOnt, fac.getRDFSLabel())) {
				    OWLAnnotationValue val = a.getValue();
				    if(val instanceof OWLLiteral) {
				      	mpathlabel = ((OWLLiteral) val).getLiteral()
				    }
				}

			def ma = fac.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/MA_"+macode))
			def mpath = fac.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/MPATH_"+mpathcode))

			//def ce = fac.getOWLObjectIntersectionOf(fac.getOWLObjectPropertyDomainAxiom(has_lesion, mpath),fac.getOWLObjectSomeValuesFrom(ma_part_of,ma))
			haslesion_mpath = fac.getOWLObjectSomeValuesFrom(has_lesion,mpath)
			def ce = fac.getOWLObjectIntersectionOf(haslesion_mpath,ma)
			ce = fac.getOWLObjectIntersectionOf(ce,L)
			def newclass = fac.getOWLClass(IRI.create("http://phenomebrowser.net/map/MAP_"+macode+"x"+mpathcode))

			label = fac.getOWLAnnotation(fac.getRDFSLabel(),fac.getOWLLiteral(malabel+" has lesion "+mpathlabel));
			axiom = fac.getOWLAnnotationAssertionAxiom(newclass.getIRI(), label)
			manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

			id = fac.getOWLAnnotation(fac.getRDFSIsDefinedBy(),fac.getOWLLiteral("MAP:"+macode+"x"+mpathcode))
			axiom = fac.getOWLAnnotationAssertionAxiom(newclass.getIRI(), id)
			manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

			fac.getOWLEquivalentClassesAxiom(newclass, ce)
			manager.addAxiom(mpathMaOnt, fac.getOWLEquivalentClassesAxiom(newclass, ce))	





			}	

		}
	}
}

OWLReasoner mpathMaReasoner = f.createReasoner(mpathMaOnt,config)
mpathMaReasoner.getSubClasses(fac.getOWLThing(), true).each { println it }
println("Number of axioms: " + mpathMaOnt.getAxiomCount());


manager.saveOntology(mpathMaOnt,IRI.create((new File("/home/sarah/groovyWorkSpace/ontology_generating/MAP_AC.owl").toURI())))

