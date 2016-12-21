package wsc.ecj.gp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jgrapht.DirectedGraph;
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
import wsc.owl.bean.OWLClass;

public class WSCInitializer extends GPInitializer {

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

	public InitialWSCPool initialWSCPool;

	public static DirectedGraph<String, DefaultEdge> ontologyDAG;
	public static final String rootconcept = "TOPNODE";

	public Map<String, double[]> serviceQoSMap = new HashMap<String, double[]>();
	public Map<String, Service> serviceMap = new HashMap<String, Service>();
	public Set<Service> relevant;
	// public Map<String, TaxonomyNode> taxonomyMap = new HashMap<String,
	// TaxonomyNode>();
	public static List<String> taskInput;
	public static List<String> taskOutput;
//	public Service startServ;
//	public Service endServ;
	public static GraphRandom random;

	public double w1;
	public double w2;
	public double w3;
	public double w4;
	public double w5;
	public double w6;

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

		w1 = state.parameters.getDouble(weight1Param, null);
		w2 = state.parameters.getDouble(weight2Param, null);
		w3 = state.parameters.getDouble(weight3Param, null);
		w4 = state.parameters.getDouble(weight4Param, null);
		w5 = state.parameters.getDouble(weight5Param, null);
		w6 = state.parameters.getDouble(weight6Param, null);

		// Initial all data related to Web service composition pools
		try {
			initialTask(state.parameters.getString(taskParam, null));
			initialWSCPool = new InitialWSCPool(state.parameters.getString(servicesParam, null),
					state.parameters.getString(taxonomyParam, null));
			System.out.println("Initial servicelist:(before removed later) "
					+ initialWSCPool.getSwsPool().getServiceList().size());

			initialWSCPool.allRelevantService(taskInput, taskOutput);

			System.out.println("All relevant service: " + initialWSCPool.getServiceSequence().size());

		} catch (JAXBException | IOException e) {
			e.printStackTrace();
		}

		MapServiceToQoS(initialWSCPool.getServiceSequence());
		// mapServicesToIndex(initialWSCPool.getServiceSequence(),
		// serviceToIndexMap);
		calculateNormalisationBounds(initialWSCPool.getServiceSequence(),
				initialWSCPool.getSemanticsPool().getOwlInstHashMap().size());

		ontologyDAG = createOntologyDAG(initialWSCPool);

		// parseWSCServiceFile(state.parameters.getString(servicesParam, null));
		// parseWSCTaskFile(state.parameters.getString(taskParam, null));
		// parseWSCTaxonomyFile(state.parameters.getString(taxonomyParam,
		// null));
		// findConceptsForInstances();

//		 double[] mockQos = new double[4];
//		 mockQos[TIME] = 0;
//		 mockQos[COST] = 0;
//		 mockQos[AVAILABILITY] = 1;
//		 mockQos[RELIABILITY] = 1;
		// Set<String> startOutput = new HashSet<String>();
		// startOutput.addAll(taskInput);
//		startServ = new Service("start", mockQos, new ArrayList<String>(), taskInput);
//		endServ = new Service("end", mockQos, taskOutput, new ArrayList<String>());

		// populateTaxonomyTree();
		// relevant = getRelevantServices(serviceMap, taskInput, taskOutput);
		// calculateNormalisationBounds(relevant);
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

	/**
	 * Checks whether set of inputs can be completely satisfied by the search
	 * set, making sure to check descendants of input concepts for the
	 * subsumption.
	 *
	 * @param inputs
	 * @param searchSet
	 * @return true if search set subsumed by input set, false otherwise.
	 */
	// public boolean isSubsumed(Set<String> inputs, Set<String> searchSet) {
	// boolean satisfied = true;
	// for (String input : inputs) {
	// Set<String> subsumed = taxonomyMap.get(input).getSubsumedConcepts();
	// if (!isIntersection( searchSet, subsumed )) {
	// satisfied = false;
	// break;
	// }
	// }
	// return satisfied;
	// }

	/**
	 * Returns the set of inputs that cannot be satisfied by the search set.
	 *
	 * @param inputs
	 * @param searchSet
	 * @return inputs not subsumed.
	 */
	// public Set<String> getInputsNotSubsumed(Set<String> inputs, Set<String>
	// searchSet) {
	// Set<String> notSatisfied = new HashSet<String>();
	// for (String input : inputs) {
	// Set<String> subsumed = taxonomyMap.get(input).getSubsumedConcepts();
	// if (!isIntersection( searchSet, subsumed )) {
	// notSatisfied.add(input);
	// }
	// }
	// return notSatisfied;
	// }
	//
	// /**
	// * Returns the set of inputs that can be satisfied by the search set.
	// *
	// * @param inputs
	// * @param searchSet
	// * @return inputs subsumed.
	// */
	// public Set<String> getInputsSubsumed(Set<String> inputs, Set<String>
	// searchSet) {
	// Set<String> satisfied = new HashSet<String>();
	// for (String input : inputs) {
	// Set<String> subsumed = taxonomyMap.get(input).getSubsumedConcepts();
	// if (isIntersection(searchSet,subsumed)) {
	// satisfied.add(input);
	// }
	// }
	// return satisfied;
	// }
	//
	// private static boolean isIntersection( Set<String> a, Set<String> b ) {
	// for ( String v1 : a ) {
	// if ( b.contains( v1 ) ) {
	// return true;
	// }
	// }
	// return false;
	// }

	/**
	 * Populates the taxonomy tree by associating services to the nodes in the
	 * tree.
	 */
	// private void populateTaxonomyTree() {
	// for (Service s: serviceMap.values()) {
	// addServiceToTaxonomyTree(s);
	// }
	// }
	//
	// private void addServiceToTaxonomyTree(Service s) {
	// // Populate outputs
	// Set<TaxonomyNode> seenConceptsOutput = new HashSet<TaxonomyNode>();
	// for (String outputVal : s.getOutputs()) {
	// TaxonomyNode n = taxonomyMap.get(outputVal);
	// s.getTaxonomyOutputs().add(n);
	//
	// // Also add output to all parent nodes
	// Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
	// queue.add( n );
	//
	// while (!queue.isEmpty()) {
	// TaxonomyNode current = queue.poll();
	// seenConceptsOutput.add( current );
	// current.servicesWithOutput.add(s);
	// for (TaxonomyNode parent : current.parents) {
	// if (!seenConceptsOutput.contains( parent )) {
	// queue.add(parent);
	// seenConceptsOutput.add(parent);
	// }
	// }
	// }
	// }
	// // Populate inputs
	// Set<TaxonomyNode> seenConceptsInput = new HashSet<TaxonomyNode>();
	// for (String inputVal : s.getInputs()) {
	// TaxonomyNode n = taxonomyMap.get(inputVal);
	//
	// // Also add input to all children nodes
	// Queue<TaxonomyNode> queue = new LinkedList<TaxonomyNode>();
	// queue.add( n );
	//
	// while(!queue.isEmpty()) {
	// TaxonomyNode current = queue.poll();
	// seenConceptsInput.add( current );
	//
	// Set<String> inputs = current.servicesWithInput.get(s);
	// if (inputs == null) {
	// inputs = new HashSet<String>();
	// inputs.add(inputVal);
	// current.servicesWithInput.put(s, inputs);
	// }
	// else {
	// inputs.add(inputVal);
	// }
	//
	// for (TaxonomyNode child : current.children) {
	// if (!seenConceptsInput.contains( child )) {
	// queue.add(child);
	// seenConceptsInput.add( child );
	// }
	// }
	// }
	// }
	// return;
	// }

	/**
	 * Converts input, output, and service instance values to their
	 * corresponding ontological parent.
	 */
	// private void findConceptsForInstances() {
	// Set<String> temp = new HashSet<String>();
	//
	// for (String s : taskInput)
	// temp.add(taxonomyMap.get(s).parents.get(0).value);
	// taskInput.clear();
	// taskInput.addAll(temp);
	//
	// temp.clear();
	// for (String s : taskOutput)
	// temp.add(taxonomyMap.get(s).parents.get(0).value);
	// taskOutput.clear();
	// taskOutput.addAll(temp);
	//
	// for (Service s : serviceMap.values()) {
	// temp.clear();
	// Set<String> inputs = s.getInputs();
	// for (String i : inputs)
	// temp.add(taxonomyMap.get(i).parents.get(0).value);
	// inputs.clear();
	// inputs.addAll(temp);
	//
	// temp.clear();
	// Set<String> outputs = s.getOutputs();
	// for (String o : outputs)
	// temp.add(taxonomyMap.get(o).parents.get(0).value);
	// outputs.clear();
	// outputs.addAll(temp);
	// }
	// }
	//
	// public void removeDanglingNodes(Graph graph) {
	// List<GraphNode> dangling = new ArrayList<GraphNode>();
	// for (GraphNode g : graph.nodeMap.values()) {
	// if (!g.getName().equals("end") && g.getOutgoingEdgeList().isEmpty())
	// dangling.add( g );
	// }
	//
	// for (GraphNode d: dangling) {
	// removeDangling(d, graph);
	// }
	// }
	//
	// private void removeDangling(GraphNode n, Graph graph) {
	// if (n.getOutgoingEdgeList().isEmpty()) {
	// graph.nodeMap.remove( n.getName() );
	// for (GraphEdge e : n.getIncomingEdgeList()) {
	// e.getFromNode().getOutgoingEdgeList().remove( e );
	// graph.edgeList.remove( e );
	// removeDangling(e.getFromNode(), graph);
	// }
	// }
	// }

	/**
	 * Goes through the service list and retrieves only those services which
	 * could be part of the composition task requested by the user.
	 *
	 * @param serviceMap
	 * @return relevant services
	 */
	// private Set<Service> getRelevantServices(Map<String,Service> serviceMap,
	// Set<String> inputs, Set<String> outputs) {
	// // Copy service map values to retain original
	// Collection<Service> services = new
	// ArrayList<Service>(serviceMap.values());
	//
	// Set<String> cSearch = new HashSet<String>(inputs);
	// Set<Service> sSet = new HashSet<Service>();
	// int layer = 0;
	// Set<Service> sFound = discoverService(services, cSearch);
	// while (!sFound.isEmpty()) {
	// sSet.addAll(sFound);
	// // Record the layer that the services belong to in each node
	// for (Service s : sFound)
	// s.layer = layer;
	//
	// layer++;
	// services.removeAll(sFound);
	// for (Service s: sFound) {
	// cSearch.addAll(s.getOutputs());
	// }
	// sFound.clear();
	// sFound = discoverService(services, cSearch);
	// }
	//
	// if (isSubsumed(outputs, cSearch)) {
	// return sSet;
	// }
	// else {
	// String message = "It is impossible to perform a composition using the
	// services and settings provided.";
	// System.out.println(message);
	// System.exit(0);
	// return null;
	// }
	// }
	//
	// private void calculateNormalisationBounds(Set<Service> services) {
	// for(Service service: services) {
	// double[] qos = service.getQos();
	//
	// // Availability
	// double availability = qos[AVAILABILITY];
	// if (availability > maxAvailability)
	// maxAvailability = availability;
	//
	// // Reliability
	// double reliability = qos[RELIABILITY];
	// if (reliability > maxReliability)
	// maxReliability = reliability;
	//
	// // Time
	// double time = qos[TIME];
	// if (time > maxTime)
	// maxTime = time;
	// if (time < minTime)
	// minTime = time;
	//
	// // Cost
	// double cost = qos[COST];
	// if (cost > maxCost)
	// maxCost = cost;
	// if (cost < minCost)
	// minCost = cost;
	// }
	// // Adjust max. cost and max. time based on the number of services in
	// shrunk repository
	// maxCost *= services.size();
	// maxTime *= services.size();
	//
	// }

	/**
	 * Discovers all services from the provided collection whose input can be
	 * satisfied either (a) by the input provided in searchSet or (b) by the
	 * output of services whose input is satisfied by searchSet (or a
	 * combination of (a) and (b)).
	 *
	 * @param services
	 * @param searchSet
	 * @return set of discovered services
	 */
	// private Set<Service> discoverService(Collection<Service> services,
	// Set<String> searchSet) {
	// Set<Service> found = new HashSet<Service>();
	// for (Service s: services) {
	// if (isSubsumed(s.getInputs(), searchSet))
	// found.add(s);
	// }
	// return found;
	// }

	/**
	 * Parses the WSC Web service file with the given name, creating Web
	 * services based on this information and saving them to the service map.
	 *
	 * @param fileName
	 */
	// private void parseWSCServiceFile(String fileName) {
	// Set<String> inputs = new HashSet<String>();
	// Set<String> outputs = new HashSet<String>();
	// Set<String> precondition = new HashSet<String>();
	// Set<String> postcondition = new HashSet<String>();
	// double[] qos = new double[4];
	//
	// try {
	// File fXmlFile = new File(fileName);
	// DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	// DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	// Document doc = dBuilder.parse(fXmlFile);
	//
	// NodeList nList = doc.getElementsByTagName("service");
	//
	// for (int i = 0; i < nList.getLength(); i++) {
	// org.w3c.dom.Node nNode = nList.item(i);
	// Element eElement = (Element) nNode;
	//
	// String name = eElement.getAttribute("name");
	// qos[TIME] = Double.valueOf(eElement.getAttribute("Res"));
	// qos[COST] = Double.valueOf(eElement.getAttribute("Pri"));
	// qos[AVAILABILITY] = Double.valueOf(eElement.getAttribute("Ava"));
	// qos[RELIABILITY] = Double.valueOf(eElement.getAttribute("Rel"));
	//
	// // Get inputs
	// org.w3c.dom.Node inputNode =
	// eElement.getElementsByTagName("inputs").item(0);
	// NodeList inputNodes =
	// ((Element)inputNode).getElementsByTagName("instance");
	// for (int j = 0; j < inputNodes.getLength(); j++) {
	// org.w3c.dom.Node in = inputNodes.item(j);
	// Element e = (Element) in;
	// inputs.add(e.getAttribute("name"));
	// }
	//
	// // Get outputs
	// org.w3c.dom.Node outputNode =
	// eElement.getElementsByTagName("outputs").item(0);
	// NodeList outputNodes =
	// ((Element)outputNode).getElementsByTagName("instance");
	// for (int j = 0; j < outputNodes.getLength(); j++) {
	// org.w3c.dom.Node out = outputNodes.item(j);
	// Element e = (Element) out;
	// outputs.add(e.getAttribute("name"));
	// }
	//
	// Service ws = new Service(name, qos, inputs, outputs);
	// serviceMap.put(name, ws);
	// inputs = new HashSet<String>();
	// outputs = new HashSet<String>();
	// qos = new double[4];
	// }
	// }
	// catch(IOException ioe) {
	// System.out.println("Service file parsing failed...");
	// }
	// catch (ParserConfigurationException e) {
	// System.out.println("Service file parsing failed...");
	// }
	// catch (SAXException e) {
	// System.out.println("Service file parsing failed...");
	// }
	// }

	/**
	 * Parses the WSC task file with the given name, extracting input and output
	 * values to be used as the composition task.
	 *
	 * @param fileName
	 */
	// private void parseWSCTaskFile(String fileName) {
	// try {
	// File fXmlFile = new File(fileName);
	// DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	// DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	// Document doc = dBuilder.parse(fXmlFile);
	//
	// org.w3c.dom.Node provided = doc.getElementsByTagName("provided").item(0);
	// NodeList providedList = ((Element)
	// provided).getElementsByTagName("instance");
	// taskInput = new HashSet<String>();
	// for (int i = 0; i < providedList.getLength(); i++) {
	// org.w3c.dom.Node item = providedList.item(i);
	// Element e = (Element) item;
	// taskInput.add(e.getAttribute("name"));
	// }
	//
	// org.w3c.dom.Node wanted = doc.getElementsByTagName("wanted").item(0);
	// NodeList wantedList = ((Element)
	// wanted).getElementsByTagName("instance");
	// taskOutput = new HashSet<String>();
	// for (int i = 0; i < wantedList.getLength(); i++) {
	// org.w3c.dom.Node item = wantedList.item(i);
	// Element e = (Element) item;
	// taskOutput.add(e.getAttribute("name"));
	// }
	// }
	// catch (ParserConfigurationException e) {
	// System.out.println("Task file parsing failed...");
	// e.printStackTrace();
	// }
	// catch (SAXException e) {
	// System.out.println("Task file parsing failed...");
	// e.printStackTrace();
	// }
	// catch (IOException e) {
	// System.out.println("Task file parsing failed...");
	// e.printStackTrace();
	// }
	// }

	/**
	 * Parses the WSC taxonomy file with the given name, building a tree-like
	 * structure.
	 *
	 * @param fileName
	 *            //
	 */
	// private void parseWSCTaxonomyFile(String fileName) {
	// try {
	// File fXmlFile = new File(fileName);
	// DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	// DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	// Document doc = dBuilder.parse(fXmlFile);
	// NodeList taxonomyRoots = doc.getChildNodes();
	//
	// processTaxonomyChildren(null, taxonomyRoots);
	// }
	//
	// catch (ParserConfigurationException e) {
	// System.err.println("Taxonomy file parsing failed...");
	// }
	// catch (SAXException e) {
	// System.err.println("Taxonomy file parsing failed...");
	// }
	// catch (IOException e) {
	// System.err.println("Taxonomy file parsing failed...");
	// }
	// }

	/**
	 * Recursive function for recreating taxonomy structure from file.
	 *
	 * @param parent
	 *            - Nodes' parent
	 * @param nodes
	 */
	// private void processTaxonomyChildren(TaxonomyNode parent, NodeList nodes)
	// {
	// if (nodes != null && nodes.getLength() != 0) {
	// for (int i = 0; i < nodes.getLength(); i++) {
	// org.w3c.dom.Node ch = nodes.item(i);
	//
	// if (!(ch instanceof Text)) {
	// Element currNode = (Element) nodes.item(i);
	// String value = currNode.getAttribute("name");
	// TaxonomyNode taxNode = taxonomyMap.get( value );
	// if (taxNode == null) {
	// taxNode = new TaxonomyNode(value);
	// taxonomyMap.put( value, taxNode );
	// }
	// if (parent != null) {
	// taxNode.parents.add(parent);
	// parent.children.add(taxNode);
	// }
	//
	// NodeList children = currNode.getChildNodes();
	// processTaxonomyChildren(taxNode, children);
	// }
	// }
	// }
	// }
}
