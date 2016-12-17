package wsc.ecj.gp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.Set;

import ec.EvolutionState;
import ec.Individual;
import ec.Species;
import ec.gp.GPNode;
import ec.util.Parameter;
import graph.GraphNode;
import wsc.graph.ServiceGraph;
import wsc.graph.ServiceEdge;

public class WSCSpecies extends Species {

	private static final long serialVersionUID = 1L;

	@Override
	public Parameter defaultBase() {
		return new Parameter("wscspecies");
	}

	@Override
	public Individual newIndividual(EvolutionState state, int thread) {
	    WSCInitializer init = (WSCInitializer) state.initializer;
		//Generate Graph
	    ServiceGraph graph = generateGraph(init);
	    state.output.println(graph.toString(), 0);
		//Generate Tree from Graph
		GPNode treeRoot= toWeightedTree("startNode", graph);
	    WSCIndividual tree = new WSCIndividual(treeRoot);
	    state.output.println(tree.toString(), 0);

	    //GPNode treeRoot = createNewTree(state, init.taskInput, init.taskOutput); // XXX

//	    System.out.println("Create tree");
//	    try {
//	    	FileWriter writer = new FileWriter(new File("debug-graph.dot"));
//			writer.append(graph.toString());
//			writer.close();
//			FileWriter writer2 = new FileWriter(new File("debug-tree.dot"));
//			writer2.append(tree.toString());
//			writer2.close();
//			System.exit(0);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	    return tree;
	}
	/**
	 * generate graph that remove all dangle nodes
	 *
	 * @return graph
	 */

	public ServiceGraph  generateGraph(WSCInitializer init){

		ServiceGraph graph = new ServiceGraph(ServiceEdge.class);

		init.initialWSCPool.createGraphService(WSCInitializer.taskInput, WSCInitializer.taskOutput, graph);

		while (true) {
			List<String> dangleVerticeList = dangleVerticeList(graph);
			if (dangleVerticeList.size() == 0) {
				break;
			}
			removeCurrentdangle(graph, dangleVerticeList);
		}

		return graph;
	}

	/**
	 * Indirectly recursive method that transforms this GraphNode and all nodes
	 * that directly or indirectly receive its output into a tree
	 * representation.
	 *
	 * @return Tree root
	 */
	public GPNode toWeightedTree(String vertice, ServiceGraph graph) {
		GPNode root = null;
		if (vertice.equals("startNode")) {
			// Start with sequence
			ServiceGPNode startService = new ServiceGPNode();
			startService.setSerName("startNode");
			GPNode rightChild;

			if (graph.outDegreeOf("startNode") == 1) {
				/*
				 * If the next node points to the output, this is a
				 * single-service composition, so return a service node
				 */

				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
				outgoingEdges.addAll(graph.outgoingEdgesOf("startNode"));
				ServiceEdge outgoingEdge = outgoingEdges.get(0);
				String nextvertice = graph.getEdgeTarget(outgoingEdge);
				rightChild = getWeightedNode(nextvertice, graph);
				root = createSequenceTopNode(startService,rightChild,graph);

			}
			// Start with parallel node
			else if (graph.outDegreeOf("startNode") > 1) {
				// root = createParallelNode(this, outgoingEdgeList);
				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
				outgoingEdges.addAll(graph.outgoingEdgesOf("startNode"));
				rightChild = createParallelNode(outgoingEdges, graph);
				root = createSequenceTopNode(startService,rightChild,graph);

			}
		} else {
			// Begin by checking how many nodes are in the right child.
			GPNode rightChild;

			List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
			outgoingEdges.addAll(graph.outgoingEdgesOf(vertice));

			// Find the end node in the list, if it is contained there
			ServiceEdge outputEdge= null;
			for(ServiceEdge outgoingedge: outgoingEdges){
				if(graph.getEdgeTarget(outgoingedge).equals("endNode")){
					outputEdge = outgoingedge;
					//Remove the output node from the children list
					outgoingEdges.remove(outputEdge);
					break;
				}
			}

			// If there is only one other child, create a sequence construct
			if (outgoingEdges.size() == 1) {
				rightChild = getWeightedNode(graph.getEdgeTarget(outgoingEdges.get(0)),graph);
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setSerName(vertice);
				root = createSequenceNode(sgp, rightChild);
			}

			// Else if there are no children at all, return a new leaf node
			else if (outgoingEdges.size() == 0) {
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setSerName(vertice);
				root = sgp;
			}
			// Else, create a new parallel construct wrapped in a sequence
			// construct
			else {
				rightChild = createParallelNode(outgoingEdges, graph);
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setSerName(vertice);
				root = createSequenceNode(sgp, rightChild);
			}

		}

		return root;
	}

	/**
	 * Indirectly recursive method that transforms this GraphNode and all nodes
	 * that directly or indirectly receive its output into a tree
	 * representation.
	 *
	 * @return Tree root
	 */
	public GPNode toTree(String vertice, ServiceGraph graph) {
		GPNode root = null;
		if (vertice.equals("startNode")) {
			// Start with sequence

			if (graph.outDegreeOf("startNode") == 1) {
				/*
				 * If the next node points to the output, this is a
				 * single-service composition, so return a service node
				 */

				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
				outgoingEdges.addAll(graph.outgoingEdgesOf("startNode"));
				ServiceEdge outgoingEdge = outgoingEdges.get(0);
				String nextvertice = graph.getEdgeTarget(outgoingEdge);
				root = getNode(nextvertice, graph);
			}
			// Start with parallel node
			else if (graph.outDegreeOf("startNode") > 1) {
				// root = createParallelNode(this, outgoingEdgeList);
				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
				outgoingEdges.addAll(graph.outgoingEdgesOf("startNode"));
				root = createParallelNode(outgoingEdges, graph);
			}
		} else {
			// Begin by checking how many nodes are in the right child.
			GPNode rightChild;

			List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
			outgoingEdges.addAll(graph.outgoingEdgesOf(vertice));

			// Find the end node in the list, if it is contained there
			ServiceEdge outputEdge= null;
			for(ServiceEdge outgoingedge: outgoingEdges){
				if(graph.getEdgeTarget(outgoingedge).equals("endNode")){
					outputEdge = outgoingedge;
					//Remove the output node from the children list
					outgoingEdges.remove(outputEdge);
					break;
				}
			}

			// If there is only one other child, create a sequence construct
			if (outgoingEdges.size() == 1) {
				rightChild = getNode(graph.getEdgeTarget(outgoingEdges.get(0)),graph);
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setSerName(vertice);
				root = createSequenceNode(sgp, rightChild);
			}

			// Else if there are no children at all, return a new leaf node
			else if (outgoingEdges.size() == 0) {
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setSerName(vertice);
				ServiceGPNode endNode = new ServiceGPNode();
				endNode.setSerName("endNode");
				root = createSequenceNode(sgp, endNode);

			}
			// Else, create a new parallel construct wrapped in a sequence
			// construct
			else {
				rightChild = createParallelNode(outgoingEdges, graph);
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setSerName(vertice);
				root = createSequenceNode(sgp, rightChild);
			}

		}

		return root;
	}
	/**
	 * Represents a GraphNode with multiple outgoing edges as a ParallelNode in
	 * the tree. The children of this node are explicitly provided as a list.
	 *
	 * @param n
	 * @param childrenGraphNodes
	 * @return parallel node
	 */
	private GPNode createParallelNode(List<ServiceEdge> outgoingEdges,
			ServiceGraph graph) {
		GPNode root = new ParallelGPNode();

		// Create subtrees for children
		int length = outgoingEdges.size();
		GPNode[] children = new GPNode[length];

		for (int i = 0; i < length; i++) {
			String nextVertice = graph.getEdgeTarget(outgoingEdges.get(i));
//			children[i] = getNode(nextVertice, graph);
			children[i] = getWeightedNode(nextVertice, graph);
			children[i].parent = root;
		}
		root.children = children;
		return root;
	}

	/**
	 * Represents a GraphNode with a single outgoing edge as a SequenceNode in
	 * the tree (edges to the endNode node are not counted). The left and right
	 * children of this node are provided as arguments. If the GraphNode also
	 * has an outgoing edge to the Output (i.e. the left child also contributes
	 * with its output to the overall sequence outputs), its values should be
	 * provided as the additionalOutput argument.
	 *
	 * @param leftChild
	 * @param rightChild
	 * @param additionalOutput
	 * @param parentInput
	 * @return sequence node
	 */
	private GPNode createSequenceNode(GPNode leftChild, GPNode rightChild) {
		SequenceGPNode root = new SequenceGPNode();
		GPNode[] children = new GPNode[2];
		children[0] = leftChild;
		children[0].parent = root;
		children[1] = rightChild;
		children[1].parent = root;

		root.children = children;
		return root;
	}
	
	
	private GPNode createSequenceTopNode(GPNode leftChild, GPNode rightChild, ServiceGraph graph) {
		SequenceGPNode root = new SequenceGPNode();
		GPNode[] children = new GPNode[2];
		children[0] = leftChild;
		children[0].parent = root;
		children[1] = rightChild;
		children[1].parent = root;

		root.children = children;
		
		List<ServiceEdge> semanticEdge = new ArrayList<ServiceEdge>();
		semanticEdge.addAll(graph.edgeSet());
		root.setSemanticEdge(semanticEdge);
		
		return root;
	}

	/**
	 * Retrieves the tree representation for the provided GraphNode, also
	 * checking if should translate to a leaf.
	 *
	 * @param n
	 * @return root of tree translation
	 */
	private GPNode getWeightedNode(String nextvertice, ServiceGraph graph) {
		GPNode result;
		if (isLeaf(nextvertice, graph)) {
			ServiceGPNode sgp = new ServiceGPNode();
			sgp.setSerName(nextvertice);
			ServiceGPNode endNode = new ServiceGPNode();
			endNode.setSerName("endNode");
			result = createSequenceNode(sgp, endNode);
		}
		// Otherwise, make next node's subtree the right child
		else
			result = toWeightedTree(nextvertice, graph);
		return result;
	}
	/**
	 * Retrieves the tree representation for the provided GraphNode, also
	 * checking if should translate to a leaf.
	 *
	 * @param n
	 * @return root of tree translation
	 */
	private GPNode getNode(String nextvertice, ServiceGraph graph) {
		GPNode result;
		if (isLeaf(nextvertice, graph)) {
			ServiceGPNode sgp = new ServiceGPNode();
			sgp.setSerName(nextvertice);
			result = sgp;
		}
		// Otherwise, make next node's subtree the right child
		else
			result = toTree(nextvertice, graph);
		return result;
	}
	/**
	 * Verify whether the GraphNode provided translates into a leaf node when
	 * converting the graph into a tree.
	 *
	 * @param node
	 * @return True if it translates into a leaf node, false otherwise
	 */
	private boolean isLeaf(String verticeName, DirectedGraph<String, ServiceEdge> graph) {
		boolean a = false;
		boolean b = false;
		if (graph.outDegreeOf(verticeName) == 1) {
			a = true;
		}

		List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
		outgoingEdges.addAll(graph.outgoingEdgesOf(verticeName));
		ServiceEdge outgoingEdge = outgoingEdges.get(0);
		String nextedge = graph.getEdgeTarget(outgoingEdge);
		if (nextedge.equals("endNode")) {
			b = true;
		}
		return a && b;
	}

//	private DirectedGraph<String, ServiceEdge> graphRepresentation(List<String> taskInput, List<String> taskOutput,WSCInitializer init) {
//
//		DirectedGraph<GraphNode, ServiceEdge> directedGraph = new DefaultDirectedGraph<GraphNode, ServiceEdge>(
//				ServiceEdge.class);
//
//		init.initialWSCPool.createGraphService(taskInput, taskOutput, directedGraph);
//
//		while (true) {
//			List<GraphNode> dangleVerticeList = dangleVerticeList(directedGraph);
//			if (dangleVerticeList.size() == 0) {
//				break;
//			}
//			removeCurrentdangle(directedGraph, dangleVerticeList);
//		}
//
//		return directedGraph;
//
//	}
	private static List<String> dangleVerticeList(DirectedGraph<String, ServiceEdge> directedGraph) {
		Set<String> allVertice = directedGraph.vertexSet();

		List<String> dangleVerticeList = new ArrayList<String>();
		for (String v : allVertice) {
			int relatedOutDegree = directedGraph.outDegreeOf(v);

			if (relatedOutDegree == 0 && !v.equals("endNode")) {
				dangleVerticeList.add(v);

			}
		}
		return dangleVerticeList;
	}

	private static void removeCurrentdangle(DirectedGraph<String, ServiceEdge> directedGraph,
			List<String> dangleVerticeList) {
		// Iterator the endTangle
		for (String danglevertice : dangleVerticeList) {

			Set<ServiceEdge> relatedEdge = directedGraph.incomingEdgesOf(danglevertice);
			Set<String> potentialTangleVerticeList = new HashSet<String>();

			for (ServiceEdge edge : relatedEdge) {
				String potentialTangleVertice = directedGraph.getEdgeSource(edge);
				// System.out.println("potentialTangleVertice:" +
				// potentialTangleVertice);
				potentialTangleVerticeList.add(potentialTangleVertice);
			}

			directedGraph.removeVertex(danglevertice);
		}
	}


//	public Graph createNewGraph(EvolutionState state, Service start, Service end, Set<Service> relevant) {
//		GraphNode startNode = new GraphNode(start);
//		GraphNode endNode = new GraphNode(end);
//
//		WSCInitializer init = (WSCInitializer) state.initializer;
//
//		Graph newGraph = new Graph();
//
//		Set<String> currentEndInputs = new HashSet<String>();
//		Map<String,GraphEdge> connections = new HashMap<String,GraphEdge>();
//
//		// Connect start node
//		connectCandidateToGraphByInputs(startNode, connections, newGraph, currentEndInputs, init);
//
//		Set<Service> seenNodes = new HashSet<Service>();
//		List<Service> candidateList = new ArrayList<Service>();
//
//		addToCandidateList(start, seenNodes, relevant, candidateList, init);
//
//		Collections.shuffle(candidateList, init.random);
//
//		finishConstructingGraph(currentEndInputs, endNode, candidateList, connections, init, newGraph, seenNodes, relevant);
//
//		return newGraph;
//	}

//	public void finishConstructingGraph(Set<String> currentEndInputs, GraphNode end, List<Service> candidateList, Map<String,GraphEdge> connections,
//	        WSCInitializer init, Graph newGraph, Set<Service> seenNodes, Set<Service> relevant) {
//
//		// While end cannot be connected to graph
//		while(!checkCandidateNodeSatisfied(init, connections, newGraph, end, end.getInputs(), null)){
//			connections.clear();
//
//            // Select node
//            int index;
//
//            candidateLoop:
//            for (index = 0; index < candidateList.size(); index++) {
//                Service candidate = candidateList.get(index);
//                // For all of the candidate inputs, check that there is a service already in the graph
//                // that can satisfy it
//
//                GraphNode candNode = new GraphNode(candidate);
//                if (!checkCandidateNodeSatisfied(init, connections, newGraph, candNode, candidate.getInputs(), null)) {
//                    connections.clear();
//                	continue candidateLoop;
//                }
//
//                // Connect candidate to graph, adding its reachable services to the candidate list
//                connectCandidateToGraphByInputs(candNode, connections, newGraph, currentEndInputs, init);
//                connections.clear();
//
//                addToCandidateList(candidate, seenNodes, relevant, candidateList, init);
//
//                break;
//            }
//
//            candidateList.remove(index);
//            Collections.shuffle(candidateList, init.random);
//        }
//
//        connectCandidateToGraphByInputs(end, connections, newGraph, currentEndInputs, init);
//        connections.clear();
////        init.removeDanglingNodes(newGraph);
//	}
//
//	private boolean checkCandidateNodeSatisfied(WSCInitializer init,
//			Map<String, GraphEdge> connections, Graph newGraph,
//			GraphNode candidate, Set<String> candInputs, Set<GraphNode> fromNodes) {
//
//		Set<String> candidateInputs = new HashSet<String>(candInputs);
//		Set<String> startIntersect = new HashSet<String>();
//
//		// Check if the start node should be considered
//		GraphNode start = newGraph.nodeMap.get("start");
//
//		if (fromNodes == null || fromNodes.contains(start)) {
//    		for(String output : start.getOutputs()) {
//    			Set<String> inputVals = init.taxonomyMap.get(output).servicesWithInput.get(candidate.getService());
//    			if (inputVals != null) {
//    				candidateInputs.removeAll(inputVals);
//    				startIntersect.addAll(inputVals);
//    			}
//    		}
//
//    		if (!startIntersect.isEmpty()) {
//    			GraphEdge startEdge = new GraphEdge(startIntersect);
//    			startEdge.setFromNode(start);
//    			startEdge.setToNode(candidate);
//    			connections.put(start.getName(), startEdge);
//    		}
//		}
//
//
//		for (String input : candidateInputs) {
//			boolean found = false;
//			for (Service s : init.taxonomyMap.get(input).servicesWithOutput) {
//			    if (fromNodes == null || fromNodes.contains(s)) {
//    				if (newGraph.nodeMap.containsKey(s.getName())) {
//    					Set<String> intersect = new HashSet<String>();
//    					intersect.add(input);
//
//    					GraphEdge mapEdge = connections.get(s.getName());
//    					if (mapEdge == null) {
//    						GraphEdge e = new GraphEdge(intersect);
//    						e.setFromNode(newGraph.nodeMap.get(s.getName()));
//    						e.setToNode(candidate);
//    						connections.put(e.getFromNode().getName(), e);
//    					} else
//    						mapEdge.getIntersect().addAll(intersect);
//
//    					found = true;
//    					break;
//    				}
//			    }
//			}
//			// If that input cannot be satisfied, move on to another candidate
//			// node to connect
//			if (!found) {
//				// Move on to another candidate
//				return false;
//			}
//		}
//		return true;
//	}
//
//	public void connectCandidateToGraphByInputs(GraphNode candidate, Map<String,GraphEdge> connections, Graph graph, Set<String> currentEndInputs, WSCInitializer init) {
//		graph.nodeMap.put(candidate.getName(), candidate);
////		 int i = graph.nodeMap.size();
//		graph.edgeList.addAll(connections.values());
//		candidate.getIncomingEdgeList().addAll(connections.values());
//
//		for (GraphEdge e : connections.values()) {
//			GraphNode fromNode = graph.nodeMap.get(e.getFromNode().getName());
//			fromNode.getOutgoingEdgeList().add(e);
//		}
//		for (String o : candidate.getOutputs()) {
//			currentEndInputs.addAll(init.taxonomyMap.get(o).endNodeInputs);
//		}
//	}
//
//	public void addToCandidateList(Service n, Set<Service> seenNode, Set<Service> relevant, List<Service> candidateList, WSCInitializer init) {
//		seenNode.add(n);
//		List<TaxonomyNode> taxonomyOutputs;
//		if (n.getName().equals("start")) {
//			taxonomyOutputs = new ArrayList<TaxonomyNode>();
//			for (String outputVal : n.getOutputs()) {
//				taxonomyOutputs.add(init.taxonomyMap.get(outputVal));
//			}
//		}
//		else
//			taxonomyOutputs = init.serviceMap.get(n.getName()).getTaxonomyOutputs();
//
//		for (TaxonomyNode t : taxonomyOutputs) {
//			// Add servicesWithInput from taxonomy node as potential candidates to be connected
//			for (Service current : t.servicesWithInput.keySet()) {
//				if (!seenNode.contains(current) && relevant.contains(current)) {
//					candidateList.add(current);
//					seenNode.add(current);
//				}
//			}
//		}
//	}

//	public GPNode createNewTree(EvolutionState state, Set<String> inputSet, Set<String> outputSet) {
//		WSCInitializer init = (WSCInitializer) state.initializer;
//
//		// Find nodes that satisfy the given output
//		Set<Service> services = new HashSet<Service>();
//		Set<String> outputsToSatisfy = new HashSet<String>(outputSet);
//
//		for (String o : outputSet) {
//			if (outputsToSatisfy.contains(o)) {
//				List<Service> candidates = init.taxonomyMap.get(o).servicesWithOutput;
//				Collections.shuffle(candidates, init.random);
//				Service chosen = null;
//				candLoop:
//				for (Service cand : candidates) {
//					if (init.relevant.contains(cand)) {
//						services.add(cand);
//						chosen = cand;
//						break candLoop;
//					}
//				}
//				outputsToSatisfy.remove(o);
//
//				// Check if other outputs can also be fulfilled by the chosen candidate, and remove them also
//				Set<String> subsumed = init.getInputsSubsumed(outputsToSatisfy, chosen.outputs);
//				outputsToSatisfy.removeAll(subsumed);
//			}
//		}
//
//		GPNode root = recCreateNewTree(init, services, inputSet, outputSet);
//		return root;
//	}

//	public GPNode recCreateNewTree(WSCInitializer init, Set<Service> services, Set<String> inputSet, Set<String> outputSet) {
//
//		GPNode root;
//		List<Service> satisfiedByStart = new ArrayList<Service>();
//
//		// Check which nodes can be fully satisfied by the inputs provided
//		checkSatisfiedByInputs(init, services, inputSet, satisfiedByStart);
//
//		// Add these inputs to the list of subtrees
//		List<GPNode> subtrees = new ArrayList<GPNode>();
//		for (Service satisfied : satisfiedByStart) {
//			ServiceGPNode servNode = new ServiceGPNode();
//			servNode.setService(satisfied);
//			subtrees.add(servNode);
//		}
//
//		// If not all nodes can be satisfied by the inputs provided
//		Map<Service, Set<Service>> predecessorMap = new HashMap<Service, Set<Service>>();
//
//		// Find predecessors in previous layers for each node, checking if start satisfies them.
//		for (Service s : services) {
//			if (!satisfiedByStart.contains(s)) {
//				Set<Service> predecessors = findPredecessors(init, inputSet, s);
//				predecessorMap.put(s, predecessors);
//			}
//		}
//
//		// For each individual node, create a subtree with a sequence node root, and the node as the right child.
//		for (Entry<Service, Set<Service>> entry : predecessorMap.entrySet()) {
//			SequenceGPNode seq = new SequenceGPNode();
//			subtrees.add(seq);
//			GPNode[] children = new GPNode[2];
//
//			// The left-hand side contains the tree for the predecessor
//			GPNode leftChild = recCreateNewTree(init, entry.getValue(), inputSet, outputSet);
//			leftChild.parent = seq;
//			children[0] = leftChild;
//
//			// The right-hand side contains the node satisfied
//			ServiceGPNode rightChild = new ServiceGPNode();
//			rightChild.setService(entry.getKey());
//			rightChild.parent = seq;
//			children[1] = rightChild;
//
//			seq.children = children;
//		}
//
//		// If more than one subtree is created, put all of them under a parallel node parent.
//		if (subtrees.size() > 1) {
//			ParallelGPNode parNode = new ParallelGPNode();
//			parNode.children = new GPNode[subtrees.size()];
//			for (int i = 0; i < parNode.children.length; i++) {
//				parNode.children[i] = subtrees.get(i);
//				parNode.children[i].parent = parNode;
//			}
//			root = parNode;
//		}
//		else if (subtrees.size() == 1){
//			root = subtrees.get(0);
//		}
//		else {
//			throw new RuntimeException("No subtrees were created when recursing!");
//		}
//
//		return root;
//	}
//
//	public int checkSatisfiedByInputs(WSCInitializer init, Set<Service> services, Set<String> inputs, List<Service> satisfiedByStart) {
//		for (Service s : services) {
//			if (init.isSubsumed(s.getInputs(), inputs))
//				satisfiedByStart.add(s);
//		}
//		return satisfiedByStart.size();
//	}

//	public Set<Service> findPredecessors(WSCInitializer init, Set<String> inputs, Service s) {
//		Set<Service> predecessors = new HashSet<Service>();
//
//		// Get only inputs that are not subsumed by the given composition inputs
//		Set<String> inputsNotSatisfied = init.getInputsNotSubsumed(s.getInputs(), inputs);
//		Set<String> inputsToSatisfy = new HashSet<String>(inputsNotSatisfied);
//
//		// Find services to satisfy all inputs
//		for (String i : inputsNotSatisfied) {
//			if (inputsToSatisfy.contains(i)) {
//				List<Service> candidates = init.taxonomyMap.get(i).servicesWithOutput;
//				Collections.shuffle(candidates, init.random);
//
//				Service chosen = null;
//				candLoop:
//				for(Service cand : candidates) {
//					if (init.relevant.contains(cand) && cand.layer < s.layer) {
//						predecessors.add(cand);
//						chosen = cand;
//						break candLoop;
//					}
//				}
//
//				inputsToSatisfy.remove(i);
//
//				// Check if other outputs can also be fulfilled by the chosen candidate, and remove them also
//				Set<String> subsumed = init.getInputsSubsumed(inputsToSatisfy, chosen.outputs);
//				inputsToSatisfy.removeAll(subsumed);
//			}
//		}
//		return predecessors;
//	}

}