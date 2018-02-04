@Grapes([
	  @Grab(group='org.slf4j', module='slf4j-simple', version='1.6.1'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.2.5'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.2.5'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.2.5'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.2.5'),
          @Grab(group='net.sourceforge.owlapi', module='org.semanticweb.hermit', version='1.3.8.413')
        ])

import org.semanticweb.owlapi.model.parameters.*
import org.semanticweb.HermiT.ReasonerFactory;
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

def cli = new CliBuilder()
cli.with {
usage: 'Self'
  h longOpt:'help', 'this information'
  o longOpt:'output-file', 'output file',args:1, required:true
  t longOpt:'transitive', 'use part of in defining specific classes', args:0, required:false
  //  t longOpt:'threads', 'number of threads', args:1
  //  k longOpt:'stepsize', 'steps before splitting jobs', arg:1
}

def opt = cli.parse(args)
if( !opt ) {
  return
}
if( opt.h ) {
    cli.usage()
    return
}

def transitive = opt.t

OWLOntologyManager manager = OWLManager.createOWLOntologyManager()
OWLDataFactory fac = manager.getOWLDataFactory()


ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor()
OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor)
ReasonerFactory rf = new ReasonerFactory()

def maOnt = manager.loadOntologyFromOntologyDocument(new File('ma.owl'))
def mpathOnt = manager.loadOntologyFromOntologyDocument(new File('mpath.owl'))

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

    //affects
    def ce = null
    if (!transitive) {
      ce = fac.getOWLObjectSomeValuesFrom(hp, fac.getOWLObjectIntersectionOf(mpath, fac.getOWLObjectSomeValuesFrom(affects, ma)))
    } else {
      ce = fac.getOWLObjectSomeValuesFrom(hp, fac.getOWLObjectIntersectionOf(mpath, fac.getOWLObjectSomeValuesFrom(affects, fac.getOWLObjectSomeValuesFrom(ma_part_of, ma))))
    }
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

OWLReasoner mpathMaReasoner = rf.createReasoner(mpathMaOnt,config)

// Add inferred classes using HermiT reasoner
List<InferredAxiomGenerator<? extends OWLAxiom>> generator = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
generator.add(new InferredSubClassAxiomGenerator());
generator.add(new InferredEquivalentClassAxiomGenerator());
InferredOntologyGenerator iog = new InferredOntologyGenerator(mpathMaReasoner, generator);
iog.fillOntology(fac, mpathMaOnt);

mpathMaReasoner.getSubClasses(fac.getOWLThing(), true).each { println it }
println("Number of axioms: " + mpathMaOnt.getAxiomCount());

manager.saveOntology(mpathMaOnt,IRI.create((new File(opt.o).toURI())))
