//package ec.pso;
//
//import wsc.InitialWSCPool;
//import wsc.data.pool.Service;
//import wsc.graph.ServiceEdge;
//import wsc.owl.bean.OWLClass;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.Set;
//
//import javax.xml.bind.JAXBException;
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.ParserConfigurationException;
//
//import org.jgrapht.DirectedGraph;
//import org.jgrapht.GraphPath;
//import org.jgrapht.Graphs;
//import org.jgrapht.alg.AllDirectedPaths;
//import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
//import org.jgrapht.graph.DefaultDirectedGraph;
//import org.jgrapht.graph.DefaultEdge;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.NodeList;
//import org.xml.sax.SAXException;
//
//public class GraphPSO {
//	// PSO settings
//	public List<Particle> swarm = new ArrayList<Particle>();
//	public static final int MAX_NUM_ITERATIONS = 100;
//	public static final int NUM_PARTICLES = 30;
//	public static final float C1 = 1.49618f;
//	public static final float C2 = 1.49618f;
//	public static final float W = 0.7298f;
//	public static final boolean dynamicNormalisation = true;
//	public static int numDimensions;
//	public static ArrayList<Long> initTime = new ArrayList<Long>();
//	public static ArrayList<Long> time = new ArrayList<Long>();
//	public static ArrayList<Double> meanFitness = new ArrayList<Double>();
//	public static ArrayList<Double> bestFitnessThisGen = new ArrayList<Double>();
//	public static ArrayList<Double> bestFitnessSoFar = new ArrayList<Double>();
//	public static String logName;
//	public static Long initialisationStartTime;
//
//	// Fitness function weights
//	public static final double W1 = 0.25;
//	public static final double W2 = 0.25;
//	public static final double W3 = 0.125;
//	public static final double W4 = 0.125;
//	public static final double W5 = 0.125;
//	public static final double W6 = 0.125;
//
//	public static double MINIMUM_COST = Double.MAX_VALUE;
//	public static double MINIMUM_TIME = Double.MAX_VALUE;
//	public static double MINIMUM_RELIABILITY = 0;
//	public static double MINIMUM_AVAILABILITY = 0;
//	public static double MINIMUM_MATCHTYPE = 0;
//	public static double MININUM_SEMANTICDISTANCE = 0;
//
//	public static double MAXIMUM_COST = Double.MIN_VALUE;
//	public static double MAXIMUM_TIME = Double.MIN_VALUE;
//	public static double MAXIMUM_RELIABILITY = Double.MIN_VALUE;
//	public static double MAXIMUM_AVAILABILITY = Double.MIN_VALUE;
//	public static double MAXINUM_MATCHTYPE = 1;
//	public static double MAXINUM_SEMANTICDISTANCE = 1;
//
//	public static List<Double> meanAvailPerGen = new ArrayList<Double>();
//	public static List<Double> meanReliaPerGen = new ArrayList<Double>();
//	public static List<Double> meanTimePerGen = new ArrayList<Double>();
//	public static List<Double> meanCostPerGen = new ArrayList<Double>();
//	public static List<Double> meanMatchTypeGen = new ArrayList<Double>();
//	public static List<Double> meanSemanticDistanceGen = new ArrayList<Double>();
//
//	public static List<Double> bestAvailThisGen = new ArrayList<Double>();
//	public static List<Double> bestReliaThisGen = new ArrayList<Double>();
//	public static List<Double> bestTimeThisGen = new ArrayList<Double>();
//	public static List<Double> bestCostThisGen = new ArrayList<Double>();
//	public static List<Double> bestMatchTypeThisGen = new ArrayList<Double>();
//	public static List<Double> bestSemanticDistanceThisGen = new ArrayList<Double>();
//
//	public static List<Double> bestAvailSoFar = new ArrayList<Double>();
//	public static List<Double> bestReliaSoFar = new ArrayList<Double>();
//	public static List<Double> bestTimeSoFar = new ArrayList<Double>();
//	public static List<Double> bestCostSoFar = new ArrayList<Double>();
//	public static List<Double> bestMatchTypeSoFar = new ArrayList<Double>();
//	public static List<Double> bestSemanticDistanceSoFar = new ArrayList<Double>();
//
//	// Constants with of order of QoS attributes
//	public static final int TIME = 0;
//	public static final int COST = 1;
//	public static final int AVAILABILITY = 2;
//	public static final int RELIABILITY = 3;
//
//	public InitialWSCPool initialWSCPool;
//	public Map<String, Integer> serviceToIndexMap = new HashMap<String, Integer>();
//	Map<String, double[]> serviceQoSMap = new HashMap<String, double[]>();
//
//	public static DirectedGraph<String, DefaultEdge> ontologyDAG;
//	public static final String rootconcept = "TOPNODE";
//	public static List<String> taskInput;
//	public static List<String> taskOutput;
//	private Random random;
//
//	// Statistics tracking
//	Map<String, Integer> nodeCount = new HashMap<String, Integer>();
//	Map<String, Integer> edgeCount = new HashMap<String, Integer>();
//
//	public static void main(String[] args) {
//
//		new GraphPSO(args[0], args[1], args[2], args[3], Long.valueOf(args[4]));
//
//	}
//
//	public GraphPSO(String lName, String taskFileName, String serviceFileName, String taxonomyFileName, long seed) {
//		initialisationStartTime = System.currentTimeMillis();
//
//		logName = lName;
//		random = new Random(seed);
//
//		// Initial all data related to Web service composition pools
//		try {
//			initialTask(taskFileName);
//			initialWSCPool = new InitialWSCPool(serviceFileName, taxonomyFileName);
//			System.out.println("Initial servicelist:(before removed later) "
//					+ initialWSCPool.getSwsPool().getServiceList().size());
//
//			initialWSCPool.allRelevantService(taskInput, taskOutput);
//
//			System.out.println("All relevant service: " + initialWSCPool.getServiceSequence().size());
//
//		} catch (JAXBException | IOException e) {
//			e.printStackTrace();
//		}
//
//		MapServiceToQoS(initialWSCPool.getServiceSequence());
//		mapServicesToIndex(initialWSCPool.getServiceSequence(), serviceToIndexMap);
////		calculateNormalisationBounds(initialWSCPool.getServiceSequence(),
////				initialWSCPool.getSemanticsPool().getOwlInstHashMap().size());
//		calculateNormalisationBounds(initialWSCPool.getServiceSequence(),
//		initialWSCPool.getSemanticsPool().getOwlInstHashMap().size());
//
//		ontologyDAG = createOntologyDAG(initialWSCPool);
//
//		numDimensions = initialWSCPool.getServiceSequence().size();
//
//		String finalGraph = runPSO();
//		writeLogs(finalGraph);
//
//	}
//
//	// ==========================================================================================================
//	//
//	// PSO METHODS
//	//
//	// ==========================================================================================================
//	/**
//	 * Conducts the particle swarm optimization.
//	 */
//	public String runPSO() {
//		// 1. Initialize the swarm
//		initializeRandomSwarm();
//
//		int i = 0;
//		Particle p;
//		DirectedGraph<String, ServiceEdge> directedGraph;
//		long initialization = System.currentTimeMillis() - initialisationStartTime;
//
//		while (i < MAX_NUM_ITERATIONS) {
//			long startTime = System.currentTimeMillis();
//			System.out.println("ITERATION " + i);
//
//			// Keep track of means
//			double meanAvailability = 0.0;
//			double meanReliability = 0.0;
//			double meanTime = 0.0;
//			double meanCost = 0.0;
//			double meanMatchType = 0.00;
//			double meanSemanticDistance = 0.0;
//			double meanFit = 0.0;
//
//			double bestAThisGen = 0.0;
//			double bestRThisGen = 0.0;
//			double bestTThisGen = Double.MAX_VALUE;
//			double bestCThisGen = Double.MAX_VALUE;
//			double bestMTThisGen = 0.0;
//			double bestSDTThisGen = 0.0;
//			double bestFitThisGen = 0.0;
//
//			// Go through all particles
//			for (int j = 0; j < NUM_PARTICLES; j++) {
//				System.out.println("\tPARTICLE " + j);
//				p = swarm.get(j);
//				directedGraph = graphRepresentation(taskInput, taskOutput, p.dimensions);
////				System.out.println(directedGraph.toString());
//
//				// 2. Evaluate fitness of particle
//				aggregationAttribute(p, directedGraph);
//
//				meanAvailability += p.getAvailability();
//				meanReliability += p.getReliability();
//				meanTime += p.getTime();
//				meanCost += p.getCost();
//				meanMatchType += p.getMatchingType();
//				meanSemanticDistance += p.getSemanticDistance();
//
//				double fit = calculateFitness(p);
//				meanFit += fit;
//
//				if (fit > bestFitThisGen) {
//					bestFitThisGen = fit;
//					bestAThisGen = p.getAvailability();
//					bestRThisGen = p.getReliability();
//					bestTThisGen = p.getTime();
//					bestCThisGen = p.getCost();
//					bestMTThisGen = p.getMatchingType();
//					bestSDTThisGen = p.getSemanticDistance();
//
//				}
//				// 3. If fitness of particle is better than Pbest, update the
//				// Pbest
//				p.updatePersonalBest();
//				// 4. If fitness of Pbest is better than Gbest, update the Gbest
//				if (p.bestFitness > Particle.globalBestFitness) {
//					Particle.globalBestFitness = p.bestFitness;
//					Particle.globalGraphString = p.getStrRepresentation();
//					Particle.globalBestDimensions = Arrays.copyOf(p.bestDimensions, p.bestDimensions.length);
//					Particle.globalBestAvailability = p.getAvailability();
//					Particle.globalBestReliability = p.getReliability();
//					Particle.globalBestTime = p.getTime();
//					Particle.globalBestCost = p.getCost();
//					Particle.globalBestMatchingType = p.getMatchingType();
//					Particle.globalBestSemanticDistance = p.getSemanticDistance();
//				}
//				// 5. Update the velocity of particle
//				updateVelocity(p);
//				// 6. Update the position of particle
//				updatePosition(p);
//
//			}
//
//			// Mean QoS
//			meanAvailPerGen.add(meanAvailability / NUM_PARTICLES);
//			meanReliaPerGen.add(meanReliability / NUM_PARTICLES);
//			meanTimePerGen.add(meanTime / NUM_PARTICLES);
//			meanCostPerGen.add(meanCost / NUM_PARTICLES);
//			meanMatchTypeGen.add(meanMatchType / NUM_PARTICLES);
//			meanSemanticDistanceGen.add(meanSemanticDistance / NUM_PARTICLES);
//			meanFitness.add(meanFit / NUM_PARTICLES);
//
//			bestFitnessThisGen.add(bestFitThisGen);
//			bestAvailThisGen.add(bestAThisGen);
//			bestReliaThisGen.add(bestRThisGen);
//			bestTimeThisGen.add(bestTThisGen);
//			bestCostThisGen.add(bestCThisGen);
//			bestMatchTypeThisGen.add(bestMTThisGen);
//			bestSemanticDistanceThisGen.add(bestSDTThisGen);
//
//			bestFitnessSoFar.add(Particle.globalBestFitness);
//			bestAvailSoFar.add(Particle.globalBestAvailability);
//			bestReliaSoFar.add(Particle.globalBestReliability);
//			bestTimeSoFar.add(Particle.globalBestTime);
//			bestCostSoFar.add(Particle.globalBestCost);
//			bestMatchTypeSoFar.add(Particle.globalBestMatchingType);
//			bestSemanticDistanceSoFar.add(Particle.globalBestSemanticDistance);
//
//			initTime.add(initialization);
//			time.add(System.currentTimeMillis() - startTime);
//			initialization = 0;
//			i++;
//
//		}
//
//		return Particle.globalGraphString;
//
//	}
//
//	/**
//	 * Initialises the swarm with random positions and velocities.
//	 */
//	public void initializeRandomSwarm() {
//		swarm.clear();
//		for (int i = 0; i < NUM_PARTICLES; i++) {
//			swarm.add(new Particle(random));
//		}
//	}
//
//	/**
//	 * Updates the velocity vector of a particle.
//	 *
//	 * @param p
//	 */
//	public void updateVelocity(Particle p) {
//		float[] vel = p.velocity;
//		float[] dim = p.dimensions;
//		float[] bestDim = p.bestDimensions;
//		float[] globalBestDim = Particle.globalBestDimensions;
//
//		for (int i = 0; i < vel.length; i++) {
//			vel[i] = (W * vel[i]) + (C1 * random.nextFloat() * (bestDim[i] - dim[i]))
//					+ (C2 * random.nextFloat() * (globalBestDim[i] - dim[i]));
//		}
//	}
//
//	/**
//	 * Updates the position (i.e. dimension vector) of a particle.
//	 *
//	 * @param p
//	 */
//	public void updatePosition(Particle p) {
//		float newValue;
//		for (int i = 0; i < numDimensions; i++) {
//			// Calculate new position for that dimension
//			newValue = p.dimensions[i] + p.velocity[i];
//			// Ensure new position is within bounds
//			if (newValue < 0.0)
//				newValue = 0.0f;
//			else if (newValue > 1.0)
//				newValue = 1.0f;
//			// Update dimension array with new value
//			p.dimensions[i] = newValue;
//		}
//	}
//
//	/**
//	 * Parses the WSC task file with the given name, extracting input and output
//	 * values to be used as the composition task.
//	 *
//	 * @param fileName
//	 */
//	private void initialTask(String fileName) {
//		try {
//			File fXmlFile = new File(fileName);
//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//			Document doc = dBuilder.parse(fXmlFile);
//
//			org.w3c.dom.Node provided = doc.getElementsByTagName("provided").item(0);
//			NodeList providedList = ((Element) provided).getElementsByTagName("instance");
//			taskInput = new ArrayList<String>();
//			for (int i = 0; i < providedList.getLength(); i++) {
//				org.w3c.dom.Node item = providedList.item(i);
//				Element e = (Element) item;
//				taskInput.add(e.getAttribute("name"));
//			}
//
//			org.w3c.dom.Node wanted = doc.getElementsByTagName("wanted").item(0);
//			NodeList wantedList = ((Element) wanted).getElementsByTagName("instance");
//			taskOutput = new ArrayList<String>();
//			for (int i = 0; i < wantedList.getLength(); i++) {
//				org.w3c.dom.Node item = wantedList.item(i);
//				Element e = (Element) item;
//				taskOutput.add(e.getAttribute("name"));
//			}
//		} catch (ParserConfigurationException e) {
//			System.out.println("Task file parsing failed...");
//			e.printStackTrace();
//		} catch (SAXException e) {
//			System.out.println("Task file parsing failed...");
//			e.printStackTrace();
//		} catch (IOException e) {
//			System.out.println("Task file parsing failed...");
//			e.printStackTrace();
//		}
//	}
//
//	private void MapServiceToQoS(List<Service> serviceList) {
//		for (Service service : serviceList) {
//			serviceQoSMap.put(service.getServiceID(), service.getQos());
//		}
//	}
//
//	private void mapServicesToIndex(List<Service> relevant, Map<String, Integer> serviceToIndexMap) {
//		int i = 0;
//		for (Service service : relevant) {
//			String serviceId = service.getServiceID();
//			serviceToIndexMap.put(serviceId, i++);
//		}
//	}
//
//	private void calculateNormalisationBounds(List<Service> services, int instSize) {
//		for (Service service : services) {
//			double[] qos = service.getQos();
//
//			// Availability
//			double availability = qos[AVAILABILITY];
//			if (availability > MAXIMUM_AVAILABILITY)
//				MAXIMUM_AVAILABILITY = availability;
//
//			// Reliability
//			double reliability = qos[RELIABILITY];
//			if (reliability > MAXIMUM_RELIABILITY)
//				MAXIMUM_RELIABILITY = reliability;
//
//			// Time
//			double time = qos[TIME];
//			if (time > MAXIMUM_TIME)
//				MAXIMUM_TIME = time;
//			if (time < MINIMUM_TIME)
//				MINIMUM_TIME = time;
//
//			// Cost
//			double cost = qos[COST];
//			if (cost > MAXIMUM_COST)
//				MAXIMUM_COST = cost;
//			if (cost < MINIMUM_COST)
//				MINIMUM_COST = cost;
//		}
//		// Adjust max. cost and max. time based on the number of services in
//		// shrunk repository
//		MAXIMUM_COST *= services.size();
//		MAXIMUM_TIME *= services.size();
////		MAXINUM_SEMANTICDISTANCE *= instSize / 2;
//
//	}
//
//	private static DirectedAcyclicGraph<String, DefaultEdge> createOntologyDAG(InitialWSCPool initialWSCPool) {
//
//		DirectedAcyclicGraph<String, DefaultEdge> g = new DirectedAcyclicGraph<String, DefaultEdge>(DefaultEdge.class);
//
//		HashMap<String, OWLClass> owlClassMap = initialWSCPool.getSemanticsPool().getOwlClassHashMap();
//
//		for (String concept : owlClassMap.keySet()) {
//			g.addVertex(concept);
//
//		}
//
//		for (OWLClass owlClass : owlClassMap.values()) {
//			if (owlClass.getSubClassOf() != null && !owlClass.getSubClassOf().equals("")) {
//				String source = owlClass.getSubClassOf().getResource().substring(1);
//				String target = owlClass.getID();
//				g.addEdge(source, target);
//			}
//		}
//		return g;
//	}
//
//	private void aggregationAttribute(Particle individual, DirectedGraph<String, ServiceEdge> directedGraph) {
//
//		double a = 1.0;
//		double r = 1.0;
//		double t = 0.0;
//		double c = 0.0;
//		double mt = 1.0;
//		double dst = 0.0; // Exact Match dst = 1 ; 0 < = dst < = 1
//
//		// set a, r, c aggregation
//		Set<String> verticeSet = directedGraph.vertexSet();
//
//		// Map<String, double[]> SerQoSMap = serviceQoSMap;
//
//		for (String v : verticeSet) {
//			if (!v.equals("startNode") && !v.equals("endNode")) {
//				double qos[] = serviceQoSMap.get(v);
//				a *= qos[AVAILABILITY];
//				r *= qos[RELIABILITY];
//				c += qos[COST];
//
//			}
//		}
//
//		// set time aggregation
//		t = getLongestPathVertexList(directedGraph, serviceQoSMap);
//
//		// set mt,dst aggregation
//
//		for (ServiceEdge serviceEdge : directedGraph.edgeSet()) {
//			mt *= serviceEdge.getAvgmt();
//			dst += serviceEdge.getAvgsdt();
//		}
//
//		individual.setMatchingType(mt);
//		individual.setSemanticDistance(dst / directedGraph.edgeSet().size());
//		individual.setAvailability(a);
//		individual.setReliability(r);
//		individual.setTime(t);
//		individual.setCost(c);
//		individual.setStrRepresentation(directedGraph.toString());
//	}
//
//	private DirectedGraph<String, ServiceEdge> graphRepresentation(List<String> taskInput, List<String> taskOutput,
//			float[] weights) {
//
//		DirectedGraph<String, ServiceEdge> directedGraph = new DefaultDirectedGraph<String, ServiceEdge>(
//				ServiceEdge.class);
//
//		initialWSCPool.createGraphService(taskInput, taskOutput, directedGraph, weights, serviceToIndexMap);
//
//		while (true) {
//			List<String> dangleVerticeList = dangleVerticeList(directedGraph);
//			if (dangleVerticeList.size() == 0) {
//				break;
//			}
//			removeCurrentdangle(directedGraph, dangleVerticeList);
//		}
//
//		return directedGraph;
//
//	}
//
//	private static List<String> dangleVerticeList(DirectedGraph<String, ServiceEdge> directedGraph) {
//		Set<String> allVertice = directedGraph.vertexSet();
//
//		List<String> dangleVerticeList = new ArrayList<String>();
//		for (String v : allVertice) {
//			int relatedOutDegree = directedGraph.outDegreeOf(v);
//
//			if (relatedOutDegree == 0 && !v.equals("endNode")) {
//				dangleVerticeList.add(v);
//
//			}
//		}
//		return dangleVerticeList;
//	}
//
//	private static void removeCurrentdangle(DirectedGraph<String, ServiceEdge> directedGraph,
//			List<String> dangleVerticeList) {
//		// Iterator the endTangle
//		for (String danglevertice : dangleVerticeList) {
//
//			Set<ServiceEdge> relatedEdge = directedGraph.incomingEdgesOf(danglevertice);
//			Set<String> potentialTangleVerticeList = new HashSet<String>();
//
//			for (ServiceEdge edge : relatedEdge) {
//				String potentialTangleVertice = directedGraph.getEdgeSource(edge);
//				// System.out.println("potentialTangleVertice:" +
//				// potentialTangleVertice);
//				potentialTangleVerticeList.add(potentialTangleVertice);
//			}
//
//			directedGraph.removeVertex(danglevertice);
//		}
//	}
//
//	public static double getLongestPathVertexList(DirectedGraph<String, ServiceEdge> g,
//			Map<String, double[]> serQoSMap) {
//		// A algorithm to find all paths
//		AllDirectedPaths<String, ServiceEdge> allPath = new AllDirectedPaths<String, ServiceEdge>(g);
//		List<GraphPath<String, ServiceEdge>> pathList = allPath.getAllPaths("startNode", "endNode", true, null);
//		double maxTime = 0;
//		double sumTime;
//
//		for (int i = 0; i < pathList.size(); i++) {
//
//			sumTime = 0;
//
//			for (String v : Graphs.getPathVertexList(pathList.get(i))) {
//				if (!v.equals("startNode") && !v.equals("endNode")) {
//					double qos[] = serQoSMap.get(v);
//					sumTime += qos[TIME];
//				}
//			}
//			if (sumTime > maxTime) {
//				maxTime = sumTime;
//			}
//
//		}
//		// return pathList.get(IndexPathLength).getEdgeList();
//		return maxTime;
//	}
//
//	private double calculateFitness(Particle individual) {
//
//		double mt = individual.getMatchingType();
//		double dst = individual.getSemanticDistance();
//		double a = individual.getAvailability();
//		double r = individual.getReliability();
//		double t = individual.getTime();
//		double c = individual.getCost();
//
//		mt = normaliseMatchType(mt);
//		dst = normaliseDistanceValue(dst);
//		a = normaliseAvailability(a);
//		r = normaliseReliability(r);
//		t = normaliseTime(t);
//		c = normaliseCost(c);
//
//		individual.fitness = ((W1 * mt) + (W2 * dst) + (W3 * a) + (W4 * r) + (W5 * t) + (W6 * c));
////		individual.fitness = ((0.25 * a) + (0.25 * r) + (0.25 * t) + (0.25 * c));
//
//
//		return individual.fitness;
//	}
//
//	private double normaliseMatchType(double matchType) {
//		if (MAXINUM_MATCHTYPE - MINIMUM_MATCHTYPE == 0.0)
//			return 1.0;
//		else
//			return (matchType - MINIMUM_MATCHTYPE) / (MAXINUM_MATCHTYPE - MINIMUM_MATCHTYPE);
//	}
//
//	private double normaliseDistanceValue(double distanceValue) {
//		if (MAXINUM_SEMANTICDISTANCE - MININUM_SEMANTICDISTANCE == 0.0)
//			return 1.0;
//		else
//			return (distanceValue - MININUM_SEMANTICDISTANCE) / (MAXINUM_SEMANTICDISTANCE - MININUM_SEMANTICDISTANCE);
//	}
//
//	public double normaliseAvailability(double availability) {
//		if (MAXIMUM_AVAILABILITY - MINIMUM_AVAILABILITY == 0.0)
//			return 1.0;
//		else
//			return (availability - MINIMUM_AVAILABILITY) / (MAXIMUM_AVAILABILITY - MINIMUM_AVAILABILITY);
//	}
//
//	public double normaliseReliability(double reliability) {
//		if (MAXIMUM_RELIABILITY - MINIMUM_RELIABILITY == 0.0)
//			return 1.0;
//		else
//			return (reliability - MINIMUM_RELIABILITY) / (MAXIMUM_RELIABILITY - MINIMUM_RELIABILITY);
//	}
//
//	public double normaliseTime(double time) {
//		if (MAXIMUM_TIME - MINIMUM_TIME == 0.0)
//			return 1.0;
//		else
//			return (MAXIMUM_TIME - time) / (MAXIMUM_TIME - MINIMUM_TIME);
//	}
//
//	public double normaliseCost(double cost) {
//		if (MAXIMUM_COST - MINIMUM_COST == 0.0)
//			return 1.0;
//		else
//			return (MAXIMUM_COST - cost) / (MAXIMUM_COST - MINIMUM_COST);
//	}
//
//	// ==========================================================================================================
//	//
//	// LOGGING METHODS
//	//
//	// ==========================================================================================================
//
//	public void writeLogs(String finalGraph) {
//		try {
//			FileWriter writer = new FileWriter(new File(logName));
//			for (int i = 0; i < bestFitnessSoFar.size(); i++) {
//				writer.append(String.format("%d %d %d %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f %f\n", i,
//						initTime.get(i), time.get(i), meanFitness.get(i), bestFitnessThisGen.get(i),
//						bestFitnessSoFar.get(i), meanAvailPerGen.get(i), meanReliaPerGen.get(i), meanTimePerGen.get(i),
//						meanCostPerGen.get(i), meanMatchTypeGen.get(i), meanSemanticDistanceGen.get(i),
//						bestAvailThisGen.get(i), bestReliaThisGen.get(i), bestTimeThisGen.get(i),
//						bestCostThisGen.get(i), bestMatchTypeThisGen.get(i), bestSemanticDistanceThisGen.get(i),
//						bestAvailSoFar.get(i), bestReliaSoFar.get(i), bestTimeSoFar.get(i), bestCostSoFar.get(i),
//						bestMatchTypeSoFar.get(i), bestSemanticDistanceSoFar.get(i)));
//			}
//			writer.append(finalGraph);
//			writer.append("\n");
//			writer.close();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//}