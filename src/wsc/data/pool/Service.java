package wsc.data.pool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.NaiveLcaFinder;
import org.jgrapht.graph.DefaultEdge;

import wsc.ecj.gp.WSCInitializer;
import wsc.graph.ParamterConn;
import wsc.graph.ServiceEdge;
import wsc.graph.ServiceInput;
import wsc.graph.ServiceOutput;
import wsc.graph.ServicePostcondition;
import wsc.graph.ServicePrecondition;
import wsc.owl.bean.OWLClass;

public class Service implements Comparable<Service> {

	public final String serviceID;
	// list of inputInstances(individuals), rather than list of input parameter.
	public double[] qos;

	private List<ServiceInput> inputList = new ArrayList<ServiceInput>();
	private List<ServiceOutput> outputList = new ArrayList<ServiceOutput>();
	private List<ServicePrecondition> preconditionList = new ArrayList<ServicePrecondition>();
	private List<ServicePostcondition> postconditionList = new ArrayList<ServicePostcondition>();

	private static List<ParamterConn> pConnList0 = new ArrayList<ParamterConn>();
	private static List<ServiceInput> inputList0 = new ArrayList<ServiceInput>();
	private static Set<String> sourceSerIdSet = new HashSet<String>();

	private double score;

	public Service(String serviceID, double[] qos, List<ServiceInput> inputList, List<ServiceOutput> arrayList) {
		super();
		this.serviceID = serviceID;
		this.qos = qos;
		this.inputList = inputList;
		this.outputList = arrayList;
	}

	public Service(String serviceID, double[] qos, List<ServiceInput> inputList, List<ServiceOutput> outputList,
			List<ServicePrecondition> preconditionList, List<ServicePostcondition> postconditionList) {
		super();
		this.serviceID = serviceID;
		this.qos = qos;
		this.inputList = inputList;
		this.outputList = outputList;
		this.preconditionList = preconditionList;
		this.postconditionList = postconditionList;
	}

	public List<ServicePrecondition> getPreconditionList() {
		return preconditionList;
	}

	public void setPreconditionList(List<ServicePrecondition> preconditionList) {
		this.preconditionList = preconditionList;
	}

	public List<ServicePostcondition> getPostconditionList() {
		return postconditionList;
	}

	public void setPostconditionList(List<ServicePostcondition> postconditionList) {
		this.postconditionList = postconditionList;
	}

	public List<ServiceInput> getInputList() {
		return inputList;
	}

	public void setInputList(List<ServiceInput> inputList) {
		this.inputList = inputList;
	}

	public List<ServiceOutput> getOutputList() {
		return outputList;
	}

	public void setOutputList(List<ServiceOutput> outputList) {
		this.outputList = outputList;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public String getServiceID() {
		return this.serviceID;
	}

	public double[] getQos() {
		return qos;
	}

	public void setQos(double[] qos) {
		this.qos = qos;
	}

	public Service(String serviceID) {
		this.serviceID = serviceID;
	}

	@Override
	public int compareTo(Service o) {
		if (score > o.score)
			return -1;
		else if (score < o.score)
			return 1;
		else
			return 0;
	}

	@Override
	public String toString() {
		return "(" + score + ", " + serviceID + ")";
	}

	/**
	 * search for all potential services matched with current inputSet
	 *
	 * @param semanticsPool
	 * @param graphOutputSetMap
	 * @param intputList
	 * @return boolean
	 */
	public boolean searchServiceMatchFromInputSet(SemanticsPool semanticsPool, HashSet<String> inputSet) {
		int relevantServiceCount = 0;
		for (String giveninput : inputSet) {
			for (int j = 0; j < this.inputList.size(); j++) {
				ServiceInput serInput = this.inputList.get(j);
				if (!serInput.isSatified()) {
					String existInput = this.inputList.get(j).getInput();
					ParamterConn pConn = semanticsPool.searchSemanticMatchFromInst(giveninput, existInput);
					boolean foundmatched = pConn.isConsidered();
					if (foundmatched) {
						serInput.setSatified(true);

//						break;// each inst can only be used for one time
					}

				}

			}

		}

		for (ServiceInput sInput : this.inputList) {
			boolean sf = sInput.isSatified();
			if (sf == true) {
				relevantServiceCount++;
			}
		}

		if (relevantServiceCount == this.inputList.size()) {
			return true;
		}

		return false;
	}

	/**
	 * search for services matched with Task inputSet
	 *
	 * @param semanticsPool
	 * @param graphOutputSetMap
	 * @param intputList
	 * @return boolean
	 */
	public boolean searchServiceGraphMatchFromInputSet(SemanticsPool semanticsPool, Service service,
			List<String> graphOutputList, DirectedGraph<String, ServiceEdge> directedGraph,
			Map<String, Service> graphOutputListMap) {
		pConnList0.clear();
		inputList0.clear();
		double summt = 0.00;
		double sumdst = 0.00;

		int inputMatchCount = 0;

		for (ServiceInput serinput : service.getInputList()) {
			serinput.setSatified(false);
			inputList0.add(serinput);
		}

		for (int i = 0; i < graphOutputList.size(); i++) {
			String giveninput = graphOutputList.get(i);
			for (int j = 0; j < inputList0.size(); j++) {
				ServiceInput serInput = inputList0.get(j);
				if (!serInput.isSatified()) {
					String existInput = inputList0.get(j).getInput();
					ParamterConn pConn = semanticsPool.searchSemanticMatchTypeFromInst(giveninput, existInput);
					boolean foundmatched = pConn.isConsidered();
					if (foundmatched) {
						serInput.setSatified(true);
						pConn.setOutputInst(giveninput);
						// if (GraphPSO.taskInput.contains(giveninput)) {
						if (WSCInitializer.taskInput.contains(giveninput)) {

							pConn.setSourceServiceID("startNode");
						} else {
							pConn.setSourceServiceID(graphOutputListMap.get(giveninput).getServiceID());
						}
						double similarity = CalculateSimilarityMeasure4Concepts(WSCInitializer.ontologyDAG, giveninput,
								existInput, semanticsPool);

						pConn.setSimilarity(similarity);
						pConnList0.add(pConn);
//						break;// each inst can only be used for one time
					}

				}

			}

		}

		for (ServiceInput sInput : inputList0) {
			boolean sf = sInput.isSatified();
			if (sf == true) {
				inputMatchCount++;
			}
		}

		if (inputMatchCount == inputList0.size()) {

			directedGraph.addVertex(service.getServiceID());
			sourceSerIdSet.clear();
			// how many sourceService are connected
			for (ParamterConn p : pConnList0) {
				String sourceSerID = p.getSourceServiceID();
				sourceSerIdSet.add(sourceSerID);
			}
			List<ServiceEdge> serEdgeList = new ArrayList<ServiceEdge>();
			// Edge are needed for each sourceService
			for (String sourceSerID : sourceSerIdSet) {
				ServiceEdge serEdge = new ServiceEdge(0, 0);
				serEdge.setSourceService(sourceSerID);
				// how many parameter connection needed for each Edge
				for (ParamterConn p : pConnList0) {
					if (p.getSourceServiceID().equals(sourceSerID)) {
						serEdge.getpConnList().add(p);
					}
				}
				// add Edge to a EdgeList to calcute each edge aggregation
				// and buld edge for graph
				serEdgeList.add(serEdge);
			}

			for (ServiceEdge edge : serEdgeList) {
				summt = 0.00;
				sumdst = 0.00;
				for (int i1 = 0; i1 < edge.getpConnList().size(); i1++) {
					ParamterConn pCo = edge.getpConnList().get(i1);
					summt += pCo.getMatchType();
					sumdst += pCo.getSimilarity();

				}
				int count = edge.getpConnList().size();
				edge.setAvgmt(summt / count);
				edge.setAvgsdt(sumdst / count);
				edge.setTargetService(service.getServiceID());
				directedGraph.addEdge(edge.getSourceService(), service.getServiceID(), edge);
			}
			return true;
		}

		return false;
	}

	/**
	 * search for services matched with defined inputSet
	 *
	 * @param semanticsPool
	 * @param graphOutputSetMap
	 * @param intputList
	 * @return boolean
	 */
	public boolean searchServiceGraphMatchFromDefinedInputSet(SemanticsPool semanticsPool, Service service,
			List<String> graphOutputList, DirectedGraph<String, ServiceEdge> directedGraph,
			Map<String, Service> graphOutputListMap, List<String> ioNodeInputs) {
		pConnList0.clear();
		inputList0.clear();
		double summt = 0.00;
		double sumdst = 0.00;

		int inputMatchCount = 0;

		for (ServiceInput serinput : service.getInputList()) {
			serinput.setSatified(false);
			inputList0.add(serinput);
		}

		for (int i = 0; i < graphOutputList.size(); i++) {
			String giveninput = graphOutputList.get(i);
			for (int j = 0; j < inputList0.size(); j++) {
				ServiceInput serInput = inputList0.get(j);
				if (!serInput.isSatified()) {
					String existInput = inputList0.get(j).getInput();
					ParamterConn pConn = semanticsPool.searchSemanticMatchTypeFromInst(giveninput, existInput);
					boolean foundmatched = pConn.isConsidered();
					if (foundmatched) {
						serInput.setSatified(true);
						pConn.setOutputInst(giveninput);
						// if (GraphPSO.taskInput.contains(giveninput)) {
						if (ioNodeInputs.contains(giveninput)) {

							pConn.setSourceServiceID("startNode");
						} else {
							pConn.setSourceServiceID(graphOutputListMap.get(giveninput).getServiceID());
						}
						double similarity = CalculateSimilarityMeasure4Concepts(WSCInitializer.ontologyDAG, giveninput,
								existInput, semanticsPool);

						pConn.setSimilarity(similarity);
						pConnList0.add(pConn);
//						break;// each inst can only be used for one time
					}

				}

			}

		}

		for (ServiceInput sInput : inputList0) {
			boolean sf = sInput.isSatified();
			if (sf == true) {
				inputMatchCount++;
			}
		}

		if (inputMatchCount == inputList0.size()) {

			directedGraph.addVertex(service.getServiceID());
			sourceSerIdSet.clear();
			// how many sourceService are connected
			for (ParamterConn p : pConnList0) {
				String sourceSerID = p.getSourceServiceID();
				sourceSerIdSet.add(sourceSerID);
			}
			List<ServiceEdge> serEdgeList = new ArrayList<ServiceEdge>();
			// Edge are needed for each sourceService
			for (String sourceSerID : sourceSerIdSet) {
				ServiceEdge serEdge = new ServiceEdge(0, 0);
				serEdge.setSourceService(sourceSerID);
				// how many parameter connection needed for each Edge
				for (ParamterConn p : pConnList0) {
					if (p.getSourceServiceID().equals(sourceSerID)) {
						serEdge.getpConnList().add(p);
					}
				}
				// add Edge to a EdgeList to calcute each edge aggregation
				// and buld edge for graph
				serEdgeList.add(serEdge);
			}

			for (ServiceEdge edge : serEdgeList) {
				summt = 0.00;
				sumdst = 0.00;
				for (int i1 = 0; i1 < edge.getpConnList().size(); i1++) {
					ParamterConn pCo = edge.getpConnList().get(i1);
					summt += pCo.getMatchType();
					sumdst += pCo.getSimilarity();

				}
				int count = edge.getpConnList().size();
				edge.setAvgmt(summt / count);
				edge.setAvgsdt(sumdst / count);
				edge.setTargetService(service.getServiceID());
				directedGraph.addEdge(edge.getSourceService(), service.getServiceID(), edge);
			}
			return true;
		}

		return false;
	}

	public static double CalculateSimilarityMeasure(DirectedGraph<String, DefaultEdge> g, String giveninput,
			String existInput, SemanticsPool semanticsPool) {

		double similarityValue;
		// find instance related concept
		OWLClass givenClass = semanticsPool.getOwlClassHashMap()
				.get(semanticsPool.getOwlInstHashMap().get(giveninput).getRdfType().getResource().substring(1));
		OWLClass relatedClass = semanticsPool.getOwlClassHashMap()
				.get(semanticsPool.getOwlInstHashMap().get(existInput).getRdfType().getResource().substring(1));

		String a = givenClass.getID();
		String b = relatedClass.getID();

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

	public static double CalculateSimilarityMeasure4Concepts(DirectedGraph<String, DefaultEdge> g, String giveninput,
			String existInput, SemanticsPool semanticsPool) {
		// find instance related concept
		OWLClass givenClass = semanticsPool.getOwlClassHashMap()
				.get(semanticsPool.getOwlInstHashMap().get(giveninput).getRdfType().getResource().substring(1));
		OWLClass relatedClass = semanticsPool.getOwlClassHashMap()
				.get(semanticsPool.getOwlInstHashMap().get(existInput).getRdfType().getResource().substring(1));

		String a = givenClass.getID();
		String b = relatedClass.getID();
//		System.out.println(giveninput+"  concept of "+a+";"+existInput+"  concept of" +b);

		double similarity = WSCInitializer.semanticMatrix.get(a, b);
		return similarity;
	}

	private static boolean isNeighbourConcept(DirectedGraph<String, DefaultEdge> g, String a, String b) {

		boolean isNeighbourConcept = false;
		Set<DefaultEdge> incomingEdgeList1 = g.incomingEdgesOf(a);
		Set<DefaultEdge> incomingEdgeList2 = g.incomingEdgesOf(b);

		for (DefaultEdge e1 : incomingEdgeList1) {
			String source1 = g.getEdgeSource(e1);
			for (DefaultEdge e2 : incomingEdgeList2) {
				String source2 = g.getEdgeSource(e2);
				if (source1.equals(source2)) {
					isNeighbourConcept = true;
				}
			}
		}

		return isNeighbourConcept;
	}

	private static int MaxDepth(DirectedGraph<String, DefaultEdge> g) {

		int depth = 0;

		Set<String> verticeset = g.vertexSet();

		// update the depth while iterator successor
		for (String v : verticeset) {
			List<String> verticeList = Graphs.successorListOf(g, v);

			if (verticeList.size() > 0) {
				depth++;
			}
		}

		return depth;

	}

}
