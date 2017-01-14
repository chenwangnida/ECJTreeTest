package wsc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.jgrapht.DirectedGraph;

import wsc.data.pool.SWSPool;
import wsc.data.pool.SemanticsPool;
import wsc.data.pool.Service;
import wsc.ecj.gp.WSCInitializer;
import wsc.graph.ParamterConn;
import wsc.graph.ServiceEdge;
import wsc.graph.ServiceOutput;

public class InitialWSCPool {

	private SWSPool swsPool;

	private final HashSet<String> outputSet = new HashSet<String>();
	private final SemanticsPool semanticsPool;
	private final List<Service> serviceSequence = new LinkedList<Service>();

	private static List<Service> serviceCandidates = new ArrayList<Service>();
	private static List<String> graphOutputList = new ArrayList<String>();
	private static Map<String, Service> graphOutputListMap = new HashMap<String, Service>();
	private static List<ParamterConn> pConnList = new ArrayList<ParamterConn>();
	private static List<ServiceOutput> taskOutputList = new ArrayList<ServiceOutput>();
	private static List<ServiceOutput> requiredOutputList = new ArrayList<ServiceOutput>();
	private static Set<String> sourceSerIdSet = new HashSet<String>();

	// set and get
	public SWSPool getSwsPool() {
		return swsPool;
	}

	public HashSet<String> getOutputSet() {
		return outputSet;
	}

	public SemanticsPool getSemanticsPool() {
		return semanticsPool;
	}

	public List<Service> getServiceSequence() {
		return serviceSequence;
	}

	/**
	 * using service file and owl file to create semantics pool and service pool
	 *
	 * @param serviceFilePath
	 * @param owlFilePath
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */

	public InitialWSCPool(String serviceFilePath, String owlFilePath) throws FileNotFoundException, JAXBException {
		this.semanticsPool = SemanticsPool.createSemanticsFromOWL(owlFilePath);
		this.swsPool = SWSPool.parseWSCServiceFile(this.semanticsPool, serviceFilePath);
	}

	/**
	 * check whether output is required by the task
	 *
	 * @param givenoutput
	 * @return
	 */
	private boolean checkOutputSet(DirectedGraph<String, ServiceEdge> directedGraph, List<String> taskOutput) {
		pConnList.clear();
		taskOutputList.clear();
		int taskMatchCount = 0;
		double summt;
		double sumdst;
		for (String taskOutputStr : taskOutput) {
			ServiceOutput taskSerOutput = new ServiceOutput(taskOutputStr, false);
			taskOutputList.add(taskSerOutput);
		}

		for (int i = 0; i < this.graphOutputList.size(); i++) {
			String outputInst = this.graphOutputList.get(i);
			for (int j = 0; j < taskOutputList.size(); j++) {
				ServiceOutput serOutputReq = taskOutputList.get(j);
				if (!serOutputReq.isSatified()) {
					String outputrequ = taskOutputList.get(j).getOutput();
					ParamterConn pConn = this.semanticsPool.searchSemanticMatchTypeFromInst(outputInst, outputrequ);
					boolean foundmatched = pConn.isConsidered();
					if (foundmatched) {
						serOutputReq.setSatified(true);
						double similarity = Service.CalculateSimilarityMeasure4Concepts(WSCInitializer.ontologyDAG,
								outputInst, outputrequ, this.semanticsPool);
						pConn.setOutputInst(outputInst);
						pConn.setOutputrequ(outputrequ);
						pConn.setSourceServiceID(graphOutputListMap.get(outputInst).getServiceID());
						pConn.setSimilarity(similarity);
						pConnList.add(pConn);
						// break;
					}
				}
			}
		}

		for (ServiceOutput tOutput : taskOutputList) {
			boolean sf = tOutput.isSatified();
			if (sf == true) {
				taskMatchCount++;
			}
		}

		if (taskMatchCount == taskOutputList.size()) {
			directedGraph.addVertex("endNode");
			sourceSerIdSet.clear();
			for (ParamterConn p : pConnList) {
				String sourceSerID = p.getSourceServiceID();
				sourceSerIdSet.add(sourceSerID);
			}

			List<ServiceEdge> serEdgeList = new ArrayList<ServiceEdge>();
			for (String sourceSerID : sourceSerIdSet) {
				ServiceEdge serEdge = new ServiceEdge(0, 0);
				serEdge.setSourceService(sourceSerID);
				for (ParamterConn p : pConnList) {
					if (p.getSourceServiceID().equals(sourceSerID)) {
						serEdge.getpConnList().add(p);
					}
				}
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
				edge.setTargetService("endNode");
				directedGraph.addEdge(edge.getSourceService(), "endNode", edge);
			}
			return true;
		}

		return false;

	}

	/**
	 * check whether output is required by the defined required Output
	 *
	 * @param givenoutput
	 * @return
	 */
	private boolean checkDefinedOutputSet(DirectedGraph<String, ServiceEdge> directedGraph,
			List<String> requiredOutput) {
		pConnList.clear();
		requiredOutputList.clear();
		int taskMatchCount = 0;
		double summt;
		double sumdst;
		for (String taskOutputStr : requiredOutput) {
			ServiceOutput taskSerOutput = new ServiceOutput(taskOutputStr, false);
			requiredOutputList.add(taskSerOutput);
		}

		for (int i = 0; i < this.graphOutputList.size(); i++) {
			String outputInst = this.graphOutputList.get(i);
			for (int j = 0; j < requiredOutputList.size(); j++) {
				ServiceOutput serOutputReq = requiredOutputList.get(j);
				if (!serOutputReq.isSatified()) {
					String outputrequ = requiredOutputList.get(j).getOutput();
					ParamterConn pConn = this.semanticsPool.searchSemanticMatchTypeFromInst(outputInst, outputrequ);
					boolean foundmatched = pConn.isConsidered();
					if (foundmatched) {
						serOutputReq.setSatified(true);
						double similarity = Service.CalculateSimilarityMeasure4Concepts(WSCInitializer.ontologyDAG,
								outputInst, outputrequ, this.semanticsPool);
						pConn.setOutputInst(outputInst);
						pConn.setOutputrequ(outputrequ);
//						if (graphOutputListMap.get(outputInst) == null) {
//							 pConn.setSourceServiceID("startNode");
						System.err.println("Inst not in the map");
						pConn.setSourceServiceID(graphOutputListMap.get(outputInst).getServiceID());
						
						pConn.setSimilarity(similarity);
						pConnList.add(pConn);
						// break inner;
					}
				}
			}
		}

		for (ServiceOutput tOutput : requiredOutputList) {
			boolean sf = tOutput.isSatified();
			if (sf == true) {
				taskMatchCount++;
			}
		}

		if (taskMatchCount == requiredOutputList.size()) {
			directedGraph.addVertex("endNode");
			sourceSerIdSet.clear();
			for (ParamterConn p : pConnList) {
				String sourceSerID = p.getSourceServiceID();
				sourceSerIdSet.add(sourceSerID);
			}

			List<ServiceEdge> serEdgeList = new ArrayList<ServiceEdge>();
			for (String sourceSerID : sourceSerIdSet) {
				ServiceEdge serEdge = new ServiceEdge(0, 0);
				serEdge.setSourceService(sourceSerID);
				for (ParamterConn p : pConnList) {
					if (p.getSourceServiceID().equals(sourceSerID)) {
						serEdge.getpConnList().add(p);
					}
				}
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
				edge.setTargetService("endNode");
				directedGraph.addEdge(edge.getSourceService(), "endNode", edge);
			}
			return true;
		}

		return false;

	}

	/**
	 * given a task associated with input and output to find a potential
	 * services
	 *
	 * @param giveninput
	 *
	 * @param givenoutput
	 *
	 */
	public void allRelevantService(List input, List output) throws JAXBException, IOException {
		this.outputSet.addAll(input);
		do {
			Service service = this.swsPool.findPossibleService(this.outputSet);
			if (service == null) {
				return;
			}
			serviceSequence.add(service);
		} while (true);
	}

	public void createGraphService(List<String> taskInput, List<String> taskOutput,
			DirectedGraph<String, ServiceEdge> directedGraph) {

		graphOutputList.clear();
		graphOutputListMap.clear();
		serviceCandidates.clear();

		graphOutputList.addAll(taskInput);

		// SWSPool swsPool = new SWSPool();

		// SetWeightsToServiceList(serviceToIndexMap, serviceSequence, weights);
		serviceCandidates.addAll(serviceSequence);
		Collections.shuffle(serviceCandidates, WSCInitializer.random);
		// Collections.sort(serviceCandidates);

		boolean goalSatisfied;

		directedGraph.addVertex("startNode");

		do {
			Service service = swsPool.createGraphService(graphOutputList, serviceCandidates, this.semanticsPool,
					directedGraph, graphOutputListMap);
			if (service == null) {
				System.err.println("No service is usable now");
				return;
			}
			goalSatisfied = this.checkOutputSet(directedGraph, taskOutput);

		} while (!goalSatisfied);

	}

	public void createGraphService4Mutation(List<String> combinedInputs, List<String> combinedOutputs,
			DirectedGraph<String, ServiceEdge> directedGraph) {

		graphOutputList.clear();
		graphOutputListMap.clear();
		serviceCandidates.clear();

		graphOutputList.addAll(combinedInputs);

		// SWSPool swsPool = new SWSPool();

		// SetWeightsToServiceList(serviceToIndexMap, serviceSequence, weights);
		serviceCandidates.addAll(serviceSequence);
		Collections.shuffle(serviceCandidates, WSCInitializer.random);
		// Collections.sort(serviceCandidates);

		boolean goalSatisfied;

		directedGraph.addVertex("startNode");

		do {
			Service service = swsPool.createGraphService4Mutation(graphOutputList, serviceCandidates,
					this.semanticsPool, directedGraph, graphOutputListMap, combinedInputs);
			if (service == null) {
				System.err.println("No service is usable now");
				return;
			}
			goalSatisfied = this.checkDefinedOutputSet(directedGraph, combinedOutputs);

		} while (!goalSatisfied);

	}

	public void createGraphService(List<String> taskInput, List<String> taskOutput,
			DirectedGraph<String, ServiceEdge> directedGraph, float[] weights, Map<String, Integer> serviceToIndexMap) {

		graphOutputList.clear();
		graphOutputListMap.clear();
		serviceCandidates.clear();

		graphOutputList.addAll(taskInput);

		// SWSPool swsPool = new SWSPool();

		SetWeightsToServiceList(serviceToIndexMap, serviceSequence, weights);
		serviceCandidates.addAll(serviceSequence);
		Collections.sort(serviceCandidates);

		boolean goalSatisfied;

		directedGraph.addVertex("startNode");

		do {
			Service service = swsPool.createGraphService(graphOutputList, serviceCandidates, this.semanticsPool,
					directedGraph, graphOutputListMap);
			if (service == null) {
				System.err.println("No service is usable now");
				return;
			}
			goalSatisfied = this.checkOutputSet(directedGraph, taskOutput);

		} while (!goalSatisfied);

	}

	private void SetWeightsToServiceList(Map<String, Integer> serviceToIndexMap, List<Service> serviceSequence,
			float[] weights) {
		// Go through all relevant nodes
		for (Service service : serviceSequence) {
			// Find the index for that node
			int index = serviceToIndexMap.get(service.getServiceID());
			service.setScore(weights[index]);
		}
	}

}
