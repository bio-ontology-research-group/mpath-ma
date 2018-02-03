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



def maOnt = manager.loadOntologyFromOntologyDocument(new File('ma.owl'))
OWLReasoner maReasoner = f.createReasoner(maOnt,config)

def mpathOnt = manager.loadOntologyFromOntologyDocument(new File('mpath.owl'))
OWLReasoner mpathReasoner = f.createReasoner(mpathOnt,config)

IRI mpathMaOntI= IRI.create("http://phenomebrowser.net/pam/")

Set<OWLOntology> onts  = [maOnt,mpathOnt]
OWLOntology mpathMaOnt = manager.createOntology(mpathMaOntI,onts)
def maID = ""
def mpathID = ""
def ma_part_of = fac.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/ma#part_of"))
def mpath_part_of = fac.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/mpath#part_of"))
manager.addAxiom(mpathMaOnt,fac.getOWLEquivalentObjectPropertiesAxiom(ma_part_of,mpath_part_of))
manager.addAxiom(mpathMaOnt,fac.getOWLTransitiveObjectPropertyAxiom(mpath_part_of))
manager.addAxiom(mpathMaOnt,fac.getOWLTransitiveObjectPropertyAxiom(ma_part_of))
manager.addAxiom(mpathMaOnt,fac.getOWLReflexiveObjectPropertyAxiom(mpath_part_of))
manager.addAxiom(mpathMaOnt,fac.getOWLReflexiveObjectPropertyAxiom(ma_part_of))

def L = fac.getOWLClass(IRI.create("http://phenomebrowser.net/pam/PAM_0000001x0"))
mpath_0 = fac.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/MPATH_0"))
ma_0000001 = fac.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/MA_0000001"))
def affects = fac.getOWLObjectProperty(IRI.create("http://phenomebrowser.net/pam/affects"))
def hp = fac.getOWLObjectProperty(IRI.create("http://phenomebrowser.net/pam/hasLesion"))
OWLAnnotation label = fac.getOWLAnnotation(fac.getRDFSLabel(),fac.getOWLLiteral("MPATH affects MA"));
OWLAxiom axiom = fac.getOWLAnnotationAssertionAxiom(L.getIRI(), label)
manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

OWLAnnotation id = fac.getOWLAnnotation(fac.getRDFSIsDefinedBy(),fac.getOWLLiteral("PAM:0000001x0"));
fac.getOWLEquivalentClassesAxiom(L, fac.getOWLObjectIntersectionOf(mpath_0,fac.getOWLObjectSomeValuesFrom(affects, ma_0000001)))
axiom = fac.getOWLAnnotationAssertionAxiom(L.getIRI(), id)
manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

def Affects = fac.getOWLClass(IRI.create("http://phenomebrowser.net/pam/MPATHAffects:0"))
label = fac.getOWLAnnotation(fac.getRDFSLabel(),fac.getOWLLiteral("MPATH Affects"));
axiom = fac.getOWLAnnotationAssertionAxiom(Affects.getIRI(), label)
manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

id = fac.getOWLAnnotation(fac.getRDFSIsDefinedBy(),fac.getOWLLiteral("MPATHAffects:0"));
axiom = fac.getOWLAnnotationAssertionAxiom(Affects.getIRI(), id)
manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

manager.addAxiom(mpathMaOnt, fac.getOWLEquivalentClassesAxiom(fac.getOWLThing(),fac.getOWLObjectSomeValuesFrom(affects, fac.getOWLThing())))
manager.addAxiom(mpathMaOnt, fac.getOWLEquivalentClassesAxiom(Affects, fac.getOWLObjectIntersectionOf(mpath_0, fac.getOWLObjectSomeValuesFrom(affects, fac.getOWLObjectSomeValuesFrom(ma_part_of,fac.getOWLThing())))))

new File("completeDataID.csv").splitEachLine(",") { line ->
  if(line[7].contains('MA:') && line[10].contains('MPATH:') )
  {
    maID = line[7].replaceAll(":","_")
    mpathID = line[10].replaceAll(":","_")
    def ma = fac.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/"+maID))
    def mpath = fac.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/"+mpathID))

    ////affects
    def ce = fac.getOWLObjectSomeValuesFrom(hp, fac.getOWLObjectIntersectionOf(mpath, fac.getOWLObjectSomeValuesFrom(affects, ma)))
	//def ce = fac.getOWLObjectSomeValuesFrom(hp, fac.getOWLObjectIntersectionOf(mpath, fac.getOWLObjectSomeValuesFrom(affects, fac.getOWLObjectSomeValuesFrom(ma_part_of, ma))))
    //    ce = fac.getOWLObjectIntersectionOf(ce,L)
    def newclass = fac.getOWLClass(IRI.create("http://phenomebrowser.net/pam/PAM_"+maID.replaceAll("MA_","")+'x'+mpathID.replaceAll("MPATH_","")))

    label = fac.getOWLAnnotation(fac.getRDFSLabel(),fac.getOWLLiteral(line[9]+" affects "+line[6]));
    axiom = fac.getOWLAnnotationAssertionAxiom(newclass.getIRI(), label)
    manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

    id = fac.getOWLAnnotation(fac.getRDFSIsDefinedBy(),fac.getOWLLiteral("PAM:"+maID.replaceAll("MA_","")+mpathID.replaceAll("MPATH_","")));
    axiom = fac.getOWLAnnotationAssertionAxiom(newclass.getIRI(), id)
    manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

    fac.getOWLEquivalentClassesAxiom(newclass, ce)
    manager.addAxiom(mpathMaOnt, fac.getOWLEquivalentClassesAxiom(newclass, ce))

    def affectclass = fac.getOWLClass(IRI.create("http://phenomebrowser.net/pam/MPATHAffects"+mpathID.replaceAll("MPATH","")))
    label = fac.getOWLAnnotation(fac.getRDFSLabel(),fac.getOWLLiteral(line[9]+" affects"));
    axiom = fac.getOWLAnnotationAssertionAxiom(affectclass.getIRI(), label)
    manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

    id = fac.getOWLAnnotation(fac.getRDFSIsDefinedBy(),fac.getOWLLiteral("MPATHAffects:"+mpathID.replaceAll("MPATH_","")));
    axiom = fac.getOWLAnnotationAssertionAxiom(affectclass.getIRI(), id)
    manager.applyChange(new AddAxiom(mpathMaOnt, axiom));

    manager.addAxiom(mpathMaOnt, fac.getOWLSubClassOfAxiom(affectclass,Affects))
    manager.addAxiom(mpathMaOnt, fac.getOWLEquivalentClassesAxiom(affectclass, fac.getOWLObjectSomeValuesFrom(hp, fac.getOWLObjectIntersectionOf(mpath, fac.getOWLObjectSomeValuesFrom(affects, fac.getOWLObjectSomeValuesFrom(ma_part_of,fac.getOWLThing()))))))

  }
}

OWLReasoner mpathMaReasoner = f.createReasoner(mpathMaOnt,config)
mpathMaReasoner.getSubClasses(fac.getOWLThing(), true).each { println it }
println("Number of axioms: " + mpathMaOnt.getAxiomCount());

manager.saveOntology(mpathMaOnt,IRI.create((new File("PAM.owl").toURI())))
