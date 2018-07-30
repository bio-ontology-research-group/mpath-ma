@Grab(group='com.github.sharispe', module='slib-sml', version='0.9.1')
@Grab(group='org.codehaus.gpars', module='gpars', version='1.1.0')

import java.util.Set;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.OWL;
import slib.graph.algo.accessor.GraphAccessor;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GraphActionExecutor;  //
import slib.graph.algo.utils.GActionType;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.conf.GraphConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.impl.graph.memory.GraphMemory;  //
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.utils.SMConstants;
import slib.sml.sm.core.utils.SMconf;  //
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo; //
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Corpus;
import slib.sml.sm.core.metrics.ic.utils.ICconf;
import slib.utils.ex.SLIB_Exception;
import groovyx.gpars.GParsPool

ontology = "PAMT"
ontologypath ="/home/sarah/groovyWorkSpace/similarities/ontologies/PAMT.owl"
ontologyuri = "http://phenomebrowser.net/pam/"
inputpath = "/home/sarah/groovyWorkSpace/results/similarities_results_fixed_healthy_mice/annotations/PAM/"
outputpath = "/home/sarah/groovyWorkSpace/results/similarities_results_fixed_healthy_mice/resulted similarities/PAMT/"

/*
ls = ["MAP_20M_M_Annotation.txt",
,"MAP_12M_F_Annotation.txt","MAP_LONG_F_Annotation.txt"
,"MAP_12M_M_Annotation.txt","MAP_6M_F_Annotation.txt"
,"MAP_6M_M_Annotation.txt","MAP_LONG_M_Annotation.txt"
,"MAP_20M_F_Annotation.txt"]
*/

ls = ["PAM_20M_M_Annotation.txt",
,"PAM_12M_F_Annotation.txt","PAM_LONG_F_Annotation.txt"
,"PAM_12M_M_Annotation.txt","PAM_6M_F_Annotation.txt"
,"PAM_6M_M_Annotation.txt","PAM_LONG_M_Annotation.txt"
,"PAM_20M_F_Annotation.txt"]

class Mouse {
  String id
  Set annotations
  public Mouse(id, annotations) {
    setId(id)
    setAnnotations(annotations)
  }
  void addAnnotation(annotation) {
    if (!annotations.contains(annotation))
    {annotations.add(annotation);}
  }
  def getAnnotations() {
    annotations
  }
}

for (String annot:ls)

{
  String mpathMaOnt = ontologypath
  URIFactory factory = URIFactoryMemory.getSingleton();
  URI graph_uri = factory.getURI(ontologyuri);
  //factory.loadNamespacePrefix("MA_MPATH", graph_uri.toString())
  //factory.loadNamespacePrefix("MA", graph_uri.toString());
  G graph = new GraphMemory(graph_uri)
  GDataConf graphconf = new GDataConf(GFormat.RDF_XML, mpathMaOnt);
  GraphLoaderGeneric.populate(graphconf, graph);
  URI virtualRoot = factory.getURI(ontologyuri+"virtualRoot")
  graph.addV(virtualRoot)
  GAction rooting = new GAction(GActionType.REROOTING)
  rooting.addParameter("root_uri", virtualRoot.stringValue())
  GraphActionExecutor.applyAction(factory, rooting, graph)

  def getURIfromMpathMa = { mpathMa ->
    def id = mpathMa.replaceAll(":","_")
    return factory.getURI(ontologyuri + id)
  }

  def sim_id = 0
  SM_Engine engine = new SM_Engine(graph)
  ICconf icConf = new IC_Conf_Topo("Sanchez", SMConstants.FLAG_ICI_SANCHEZ_2011);
  Map<URI, Double> ics = engine.computeIC(icConf);	//what?
  Set<URI> nodes = ics.keySet();						//what?

  def getMice = {
    def mice = []
    def i = 0
    new File(inputpath+annot).splitEachLine('\t') { items ->
      def s = 0
      mice.push(new Mouse(items[0], new LinkedHashSet()))
      for (int j = 1; j < items.size(); j++) {
        URI uri = getURIfromMpathMa(items[j]);
        if (nodes.contains(uri)) {
          mice[i].addAnnotation(getURIfromMpathMa(items[j]))
        }
      }
      i++
    }
    return mice
  }

  mice = getMice()
  noann = 0

  //just to make sure all mice have annotations 
  for (i = 0; i < mice.size(); i++) {
  	if(mice[i].getAnnotations().size()==0)
  	{
  		noann ++
  		a = mice[i].id
      println("one mouse found")
  		println(mice[i].id)
  		mice.remove(i)
  	}
    	//println(mice[i].id)
    	//println(mice[i].getAnnotations())
  }


  String[] flags = [
    // SMConstants.FLAG_SIM_GROUPWISE_AVERAGE,
    // SMConstants.FLAG_SIM_GROUPWISE_AVERAGE_NORMALIZED_GOSIM,
    SMConstants.FLAG_SIM_GROUPWISE_BMA,
    SMConstants.FLAG_SIM_GROUPWISE_BMM,
    SMConstants.FLAG_SIM_GROUPWISE_MAX,
    SMConstants.FLAG_SIM_GROUPWISE_MIN,
    SMConstants.FLAG_SIM_GROUPWISE_MAX_NORMALIZED_GOSIM
  ]
  // List<String> pairFlags = new ArrayList<String>(SMConstants.PAIRWISE_MEASURE_FLAGS);
  String[] pairFlags = [
    SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_RESNIK_1995,
    SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_SCHLICKER_2006,
    SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_LIN_1998,
    SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_JIANG_CONRATH_1997_NORM
  ]
  String flagGroupwise = flags[sim_id.intdiv(pairFlags.size())];
  String flagPairwise = pairFlags[sim_id % pairFlags.size()];
  SMconf smConfGroupwise = new SMconf(flagGroupwise);
  SMconf smConfPairwise = new SMconf(flagPairwise);
  smConfPairwise.setICconf(icConf);

  // Schlicker indirect
  ICconf prob = new IC_Conf_Topo(SMConstants.FLAG_ICI_PROB_OCCURENCE_PROPAGATED);
  smConfPairwise.addParam("ic_prob", prob);


  def result = new Double[mice.size()][mice.size()]
  for (i = 0; i < mice.size(); i++) {
    for (j = 0; i < mice.size(); i++) {
    result[i][j] = 0
  }
  }

  for (i = 0; i < mice.size(); i++) {
    for (j = 0; j < mice.size(); j++) {
    result[i][j]=engine.compare(
                smConfGroupwise,smConfPairwise,
                mice[i].getAnnotations(),
                mice[j].getAnnotations())
  }
  }




  def fout = new PrintWriter(new BufferedWriter(
    new FileWriter(outputpath+annot)))
  fout.print("mice\t")
  for (i = 0; i < mice.size(); i++)
  {
  	fout.print(mice[i].id +"\t")
  }
  fout.print("\n")
  for (i = 0; i < mice.size(); i++) {
  	fout.print(mice[i].id +"\t")
    for (j = 0; j < mice.size(); j++) {
    fout.print(result[i][j]+"\t")
  }
  fout.print("\n")
  }
  fout.flush()
  fout.close()

}

