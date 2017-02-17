package wsc.ecj.gp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.NaiveLcaFinder;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ec.EvolutionState;
import ec.gp.GPInitializer;
import ec.util.Parameter;
import wsc.InitialWSCPool;
import wsc.data.pool.Service;
import wsc.graph.GraphRandom;
import wsc.graph.ServiceInput;
import wsc.graph.ServiceOutput;
import wsc.owl.bean.OWLClass;

public class WSCInitializer extends GPInitializer {



	//temporary crossoverTimes
//	public int crossoverTimess=0;

	private static final long serialVersionUID = 1L;
	// Constants with of order of QoS attributes
	public static final int TIME = 0;
	public static final int COST = 1;
	public static final int AVAILABILITY = 2;
	public static final int RELIABILITY = 3;

	public static double MINIMUM_COST = Double.MAX_VALUE;
	public static double MINIMUM_TIME = Double.MAX_VALUE;
	public static double MINIMUM_RELIABILITY = 0;
	public static double MINIMUM_AVAILABILITY = 0;
	public static double MINIMUM_MATCHTYPE = 0;
	public static double MININUM_SEMANTICDISTANCE = 0;

	public static double MAXIMUM_COST = Double.MIN_VALUE;
	public static double MAXIMUM_TIME = Double.MIN_VALUE;
	public static double MAXIMUM_RELIABILITY = Double.MIN_VALUE;
	public static double MAXIMUM_AVAILABILITY = Double.MIN_VALUE;
	public static double MAXINUM_MATCHTYPE = 1;
	public static double MAXINUM_SEMANTICDISTANCE = 1;

	public static InitialWSCPool initialWSCPool;

	public static DirectedGraph<String, DefaultEdge> ontologyDAG;
	public static final String rootconcept = "TOPNODE";

	public Map<String, double[]> serviceQoSMap = new HashMap<String, double[]>();
	public static Map<String, Service> serviceMap = new HashMap<String, Service>();
	public static Table<String, String, Double> semanticMatrix;

	// public Set<Service> relevant;
	// public Map<String, TaxonomyNode> taxonomyMap = new HashMap<String,
	// TaxonomyNode>();
	public static List<String> taskInput;
	public static List<String> taskOutput;
	// public Service startServ;
	// public Service endServ;
	public static GraphRandom random;

	public double w1;
	public double w2;
	public double w3;
	public double w4;
	public double w5;
	public double w6;

	public static InitialWSCPool getInitialWSCPool() {
		return initialWSCPool;
	}

	public void setInitialWSCPool(InitialWSCPool initialWSCPool) {
		WSCInitializer.initialWSCPool = initialWSCPool;
	}

	@Override
	public void setup(EvolutionState state, Parameter base) {

		random = new GraphRandom(state.random[0]);
		super.setup(state, base);

		Parameter servicesParam = new Parameter("composition-services");
		Parameter taskParam = new Parameter("composition-task");
		Parameter taxonomyParam = new Parameter("composition-taxonomy");
		Parameter weight1Param = new Parameter("fitness-weight1");
		Parameter weight2Param = new Parameter("fitness-weight2");
		Parameter weight3Param = new Parameter("fitness-weight3");
		Parameter weight4Param = new Parameter("fitness-weight4");
		Parameter weight5Param = new Parameter("fitness-weight5");
		Parameter weight6Param = new Parameter("fitness-weight6");

		String servicesParamStr = state.parameters.getStringWithDefault(servicesParam, null, null);
		String taskParamStr = state.parameters.getStringWithDefault(taskParam, null, null);
		String taxonomyParamStr = state.parameters.getStringWithDefault(taxonomyParam, null, null);

		w1 = state.parameters.getDouble(weight1Param, null);
		w2 = state.parameters.getDouble(weight2Param, null);
		w3 = state.parameters.getDouble(weight3Param, null);
		w4 = state.parameters.getDouble(weight4Param, null);
		w5 = state.parameters.getDouble(weight5Param, null);
		w6 = state.parameters.getDouble(weight6Param, null);

		try {
			// register task
			initialTask(taskParamStr);

			// register web services associated related ontology
			initialWSCPool = new InitialWSCPool(servicesParamStr, taxonomyParamStr);
			// System.out.println("All service: " +
			// initialWSCPool.getSwsPool().getServiceList().size());
			// construct ontology tree structure
			ontologyDAG = createOntologyDAG(initialWSCPool);
			// construct matrix storing all semantic quality for query
			semanticMatrix = HashBasedTable.create();
			// Filter web services in repository
			initialWSCPool.allRelevantService(taskInput, taskOutput);
			// System.out.println("All relevant service: " +
			// initialWSCPool.getServiceSequence().size()
			// + ";semanticMatrix: " + semanticMatrix.size());

		} catch (JAXBException | IOException e) {
			e.printStackTrace();
		}

		createSemanticMatrix();

		MapServiceToQoS(initialWSCPool.getServiceSequence());
		// mapServicesToIndex(initialWSCPool.getServiceSequence(),
		// serviceToIndexMap);
		calculateNormalisationBounds(initialWSCPool.getServiceSequence(),
				initialWSCPool.getSemanticsPool().getOwlInstHashMap().size());

	}

	/**
	 * All parameter-related concepts are converted and pre-calculated semantic
	 * quality with their subsumed existing concepts
	 *
	 * @param fileName
	 */

	private void createSemanticMatrix() {

		Set<OWLClass> parameterconcepts = new HashSet<OWLClass>();

		// Load all parameter-related concepts from task-relevant web services

		for (Service ser : initialWSCPool.getServiceSequence()) {

			for (ServiceInput serInput : ser.getInputList()) {
				OWLClass pConcept = initialWSCPool.getSemanticsPool().getOwlClassHashMap()
						.get(initialWSCPool.getSemanticsPool().getOwlInstHashMap().get(serInput.getInput()).getRdfType()
								.getResource().substring(1));
				parameterconcepts.add(pConcept);

			}

			for (ServiceOutput serOutput : ser.getOutputList()) {

				OWLClass pConcept = initialWSCPool.getSemanticsPool().getOwlClassHashMap()
						.get(initialWSCPool.getSemanticsPool().getOwlInstHashMap().get(serOutput.getOutput())
								.getRdfType().getResource().substring(1));
				parameterconcepts.add(pConcept);

			}
		}

		// Load all parameter-related concepts from given task input and
		// required output

		for (String tskInput : WSCInitializer.taskInput) {
			OWLClass pConcept = initialWSCPool.getSemanticsPool().getOwlClassHashMap().get(initialWSCPool
					.getSemanticsPool().getOwlInstHashMap().get(tskInput).getRdfType().getResource().substring(1));
			parameterconcepts.add(pConcept);
		}

		for (String tskOutput : WSCInitializer.taskOutput) {
			OWLClass pConcept = initialWSCPool.getSemanticsPool().getOwlClassHashMap().get(initialWSCPool
					.getSemanticsPool().getOwlInstHashMap().get(tskOutput).getRdfType().getResource().substring(1));
			parameterconcepts.add(pConcept);
		}

		// System.out.println("All concepts involved in semantic calcu NO.: " +
		// parameterconcepts.size());

		for (OWLClass pCon : parameterconcepts) {
			for (OWLClass pCon0 : parameterconcepts) {
				// if the pCon or PCon all parent class equal to pCon0
				if (initialWSCPool.getSemanticsPool().isSemanticMatchFromConcept(pCon, pCon0)) {

					double similarity = CalculateSimilarityMeasure(WSCInitializer.ontologyDAG, pCon.getID(),
							pCon0.getID());

					semanticMatrix.put(pCon.getID(), pCon0.getID(), similarity);
				}
				// System.out.println(
				// "givenInput: " + pCon + " existInput: " + pCon0 + " Semantic
				// Quality: " + similarity);
			}
		}

		// if (WSCInitializer.semanticMatrix.get(giveninput, existInput)==null){
		// similarity = WSCInitializer.semanticMatrix.get(existInput,
		// giveninput);
		// }else{
		// similarity = WSCInitializer.semanticMatrix.get(giveninput,
		// existInput);
		// }

	}

	public static double CalculateSimilarityMeasure(DirectedGraph<String, DefaultEdge> g, String a, String b) {

		double similarityValue;
		// find instance related concept
		// OWLClass givenClass = semanticsPool.getOwlClassHashMap()
		// .get(semanticsPool.getOwlInstHashMap().get(giveninput).getRdfType().getResource().substring(1));
		// OWLClass relatedClass = semanticsPool.getOwlClassHashMap()
		// .get(semanticsPool.getOwlInstHashMap().get(existInput).getRdfType().getResource().substring(1));
		//
		// String a = givenClass.getID();
		// String b = relatedClass.getID();

		// find the lowest common ancestor
		String lca = new NaiveLcaFinder<String, DefaultEdge>(g).findLca(a, b);

		double N = new DijkstraShortestPath(g, WSCInitializer.rootconcept, lca).getPathLength();
		double N1 = new DijkstraShortestPath(g, WSCInitializer.rootconcept, a).getPathLength();
		double N2 = new DijkstraShortestPath(g, WSCInitializer.rootconcept, b).getPathLength();

		double sim = 2 * N / (N1 + N2);
		// System.out.println("SemanticDistance:" + sim + "
		// ##################");
		//
		// if (isNeighbourConcept(g, a, b) == true) {
		// double L = new DijkstraShortestPath(g, lca, a).getPathLength()
		// + new DijkstraShortestPath(g, lca, b).getPathLength();
		//
		// int D = MaxDepth(g) + 1;
		// int r = 1;
		// double simNew = 2 * N * (Math.pow(Math.E, -r * L / D)) / (N1 + N2);
		// // System.out.println("SemanticDistance2:" + simNew + "
		// // ##################");
		// similarityValue = simNew;
		// } else {
		// similarityValue = sim;
		// }

		return sim;
	}

	/**
	 * Parses the WSC task file with the given name, extracting input and output
	 * values to be used as the composition task.
	 *
	 * @param fileName
	 */
	private void initialTask(String fileName) {
		try {
			File fXmlFile = new File(fileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			org.w3c.dom.Node provided = doc.getElementsByTagName("provided").item(0);
			NodeList providedList = ((Element) provided).getElementsByTagName("instance");
			taskInput = new ArrayList<String>();
			for (int i = 0; i < providedList.getLength(); i++) {
				org.w3c.dom.Node item = providedList.item(i);
				Element e = (Element) item;
				taskInput.add(e.getAttribute("name"));
			}

			org.w3c.dom.Node wanted = doc.getElementsByTagName("wanted").item(0);
			NodeList wantedList = ((Element) wanted).getElementsByTagName("instance");
			taskOutput = new ArrayList<String>();
			for (int i = 0; i < wantedList.getLength(); i++) {
				org.w3c.dom.Node item = wantedList.item(i);
				Element e = (Element) item;
				taskOutput.add(e.getAttribute("name"));
			}
		} catch (ParserConfigurationException e) {
			System.out.println("Task file parsing failed...");
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("Task file parsing failed...");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Task file parsing failed...");
			e.printStackTrace();
		}
	}

	private void MapServiceToQoS(List<Service> serviceList) {
		for (Service service : serviceList) {
			service.getInputList().forEach(input->input.setServiceId(service.getServiceID()));
			service.getOutputList().forEach(output->output.setServiceId(service.getServiceID()));
			serviceMap.put(service.getServiceID(), service);
			serviceQoSMap.put(service.getServiceID(), service.getQos());
		}
		
	}

	private void calculateNormalisationBounds(List<Service> services, int instSize) {
		for (Service service : services) {
			double[] qos = service.getQos();

			// Availability
			double availability = qos[AVAILABILITY];
			if (availability > MAXIMUM_AVAILABILITY)
				MAXIMUM_AVAILABILITY = availability;

			// Reliability
			double reliability = qos[RELIABILITY];
			if (reliability > MAXIMUM_RELIABILITY)
				MAXIMUM_RELIABILITY = reliability;

			// Time
			double time = qos[TIME];
			if (time > MAXIMUM_TIME)
				MAXIMUM_TIME = time;
			if (time < MINIMUM_TIME)
				MINIMUM_TIME = time;

			// Cost
			double cost = qos[COST];
			if (cost > MAXIMUM_COST)
				MAXIMUM_COST = cost;
			if (cost < MINIMUM_COST)
				MINIMUM_COST = cost;
		}
		// Adjust max. cost and max. time based on the number of services in
		// shrunk repository
		MAXIMUM_COST *= services.size();
		MAXIMUM_TIME *= services.size();
		// MAXINUM_SEMANTICDISTANCE *= instSize / 2;

	}

	private static DirectedAcyclicGraph<String, DefaultEdge> createOntologyDAG(InitialWSCPool initialWSCPool) {

		DirectedAcyclicGraph<String, DefaultEdge> g = new DirectedAcyclicGraph<String, DefaultEdge>(DefaultEdge.class);

		HashMap<String, OWLClass> owlClassMap = initialWSCPool.getSemanticsPool().getOwlClassHashMap();

		for (String concept : owlClassMap.keySet()) {
			g.addVertex(concept);

		}

		for (OWLClass owlClass : owlClassMap.values()) {
			if (owlClass.getSubClassOf() != null && !owlClass.getSubClassOf().equals("")) {
				String source = owlClass.getSubClassOf().getResource().substring(1);
				String target = owlClass.getID();
				g.addEdge(source, target);
			}
		}
		return g;
	}
}
