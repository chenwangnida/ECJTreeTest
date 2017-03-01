package wsc.ecj.gp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.jgrapht.DirectedGraph;

import java.util.Set;

import ec.EvolutionState;
import ec.Individual;
import ec.Species;
import ec.gp.GPNode;
import ec.util.Parameter;
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
		// Generate Graph
		ServiceGraph graph = generateGraph(init);
		// state.output.println(graph.toString(), 0);
		// Generate Tree from Graph
		GPNode treeRoot = toSemanticTree2("startNode", graph);
		WSCIndividual tree = new WSCIndividual(treeRoot);
		String Str = "digraph x { 833272193";
		if (tree.toString().startsWith(Str)) {
			state.output.println(tree.toString(), 0);
		}
		// GPNode treeRoot = createNewTree(state, init.taskInput,
		// init.taskOutput); // XXX

		// System.out.println("Create tree");
		// try {
		// FileWriter writer = new FileWriter(new File("debug-graph.dot"));
		// writer.append(graph.toString());
		// writer.close();
		// FileWriter writer2 = new FileWriter(new File("debug-tree.dot"));
		// writer2.append(tree.toString());
		// writer2.close();
		// System.exit(0);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		return tree;
	}

	/**
	 * generate graph that remove all dangle nodes
	 *
	 * @return graph
	 */

	public ServiceGraph generateGraph(WSCInitializer init) {

		ServiceGraph graph = new ServiceGraph(ServiceEdge.class);

		WSCInitializer.initialWSCPool.createGraphService(WSCInitializer.taskInput, WSCInitializer.taskOutput, graph);

		while (true) {
			List<String> dangleVerticeList = dangleVerticeList(graph);
			if (dangleVerticeList.size() == 0) {
				break;
			}
			removeCurrentdangle(graph, dangleVerticeList);
		}
		graph.removeEdge("startNode", "endNode");
		return graph;
	}

	public ServiceGraph Graph4Mutation(WSCInitializer init, List<String> combinedInputs, List<String> combinedOuputs) {

		ServiceGraph graph = new ServiceGraph(ServiceEdge.class);

		WSCInitializer.initialWSCPool.createGraphService4Mutation(combinedInputs, combinedOuputs, graph);
		// init.initialWSCPool.createGraphService(WSCInitializer.taskInput,
		// WSCInitializer.taskOutput, graph);

		while (true) {
			List<String> dangleVerticeList = dangleVerticeList(graph);
			if (dangleVerticeList.size() == 0) {
				break;
			}
			removeCurrentdangle(graph, dangleVerticeList);
		}

		graph.removeEdge("startNode", "endNode");

		return graph;
	}

	// public ServiceGraph Graph4Mutation(WSCInitializer init, List<String>
	// combinedInputs, List<String> combinedOuputs, Map<String, String>
	// Inst2TargetSerMap) {
	//
	// ServiceGraph graph = new ServiceGraph(ServiceEdge.class);
	//
	// init.initialWSCPool.createGraphService4Mutation(combinedInputs,
	// combinedOuputs, graph, Inst2TargetSerMap);
	// // init.initialWSCPool.createGraphService(WSCInitializer.taskInput,
	// // WSCInitializer.taskOutput, graph);
	//
	// while (true) {
	// List<String> dangleVerticeList = dangleVerticeList(graph);
	// if (dangleVerticeList.size() == 0) {
	// break;
	// }
	// removeCurrentdangle(graph, dangleVerticeList);
	// }
	//
	// graph.removeEdge("startNode", "endNode");
	//
	// return graph;
	// }
	/**
	 * Indirectly recursive method that transforms this GraphNode and all nodes
	 * that directly or indirectly receive its output into a tree representation
	 * considering semantic information that both startNodes and endNodes are
	 * included
	 *
	 * @return Tree root
	 */
	public GPNode toSemanticTree(String vertice, ServiceGraph graph) {
		GPNode root = null;
		if (vertice.equals("startNode")) {
			// Start with sequence
			// ServiceGPNode startService = new ServiceGPNode();
			// startService.setSerName("startNode");
			GPNode rightChild;

			if (graph.outDegreeOf("startNode") == 1) {
				/*
				 * If the next node points to the output, this is a
				 * single-service composition, so return a service node
				 */

				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
				outgoingEdges.addAll(graph.outgoingEdgesOf("startNode"));
				// create startNode associated with all outgoing edges
				ServiceGPNode startService = new ServiceGPNode();
				startService.setSerName("startNode");

				ServiceEdge outgoingEdge = outgoingEdges.get(0);
				String nextvertice = graph.getEdgeTarget(outgoingEdge);
				rightChild = getWeightedNode(nextvertice, graph);
				root = createSequenceTopNode(startService, rightChild, graph);

			}
			// Start with parallel node
			else if (graph.outDegreeOf("startNode") > 1) {
				// root = createParallelNode(this, outgoingEdgeList);
				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
				outgoingEdges.addAll(graph.outgoingEdgesOf("startNode"));

				// create startNode associated with all outgoing edges
				ServiceGPNode startService = new ServiceGPNode();
				startService.setSerName("startNode");

				rightChild = createParallelNode(outgoingEdges, graph);
				// root = createSequenceTopNode(startService, rightChild,
				// graph);
				root = createSequenceNode(startService, rightChild);

			}
		} else {
			// Begin by checking how many nodes are in the right child.
			GPNode rightChild;

			List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
			outgoingEdges.addAll(graph.outgoingEdgesOf(vertice));

			// Find the end node in the list, if it is contained there
			ServiceEdge outputEdge = null;
			for (ServiceEdge outgoingedge : outgoingEdges) {
				if (graph.getEdgeTarget(outgoingedge).equals("endNode")) {
					outputEdge = outgoingedge;
					// Create sequenceNode associated with endNode
					List<ServiceEdge> outgoingEdgeSet = new ArrayList<ServiceEdge>();
					outgoingEdgeSet.add(outputEdge);
					ServiceGPNode sgp = new ServiceGPNode();
					sgp.setSerName(vertice);
					ServiceGPNode endNode = new ServiceGPNode();
					endNode.setSerName("endNode");
					root = createSequenceNode(sgp, endNode);

					// Remove the output node from the children list

					outgoingEdges.remove(outputEdge);
					break;
				}
			}

			// If there is only one other child, create a sequence construct
			if (outgoingEdges.size() == 1) {
				rightChild = getWeightedNode(graph.getEdgeTarget(outgoingEdges.get(0)), graph);

				// Set<ServiceEdge> outgoingEdgeSet = new
				// HashSet<ServiceEdge>(outgoingEdges);
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setSerName(vertice);
				root = createSequenceNode(sgp, rightChild);
			}

			// Else if there are no children at all, return a new leaf node
			// else if (outgoingEdges.size() == 0) {
			// List<ServiceEdge> outgoingEdges2End = new
			// ArrayList<ServiceEdge>(outgoingEdges);
			// outgoingEdges2End.addAll(graph.getAllEdges(vertice, "endNode"));
			//
			// ServiceGPNode sgp = new ServiceGPNode(outgoingEdges2End);
			// sgp.setSerName(vertice);
			// ServiceGPNode endNode = new ServiceGPNode();
			// endNode.setSerName("endNode");
			// root = createSequenceNode(sgp, endNode);
			// }
			// Else, create a new parallel construct wrapped in a sequence
			// construct
			else if (outgoingEdges.size() > 1) {
				rightChild = createParallelNode(outgoingEdges, graph);

				// Set<ServiceEdge> outgoingEdgeSet = new
				// HashSet<ServiceEdge>(outgoingEdges);

				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setSerName(vertice);
				root = createSequenceNode(sgp, rightChild);
			}

		}

		return root;
	}

	public GPNode toSemanticTree2(String vertice, ServiceGraph graph) {
		GPNode root = null;
		if (vertice.equals("startNode")) {

			GPNode rightChild;

			if (graph.outDegreeOf("startNode") == 1) {
				/*
				 * If the next node points to the output, this is a
				 * single-service composition, so return a service node
				 */

				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
				outgoingEdges.addAll(graph.outgoingEdgesOf("startNode"));
				// create startNode associated with all outgoing edges
				ServiceGPNode startService = new ServiceGPNode();
				startService.setSerName("startNode");

				ServiceEdge outgoingEdge = outgoingEdges.get(0);
				String nextvertice = graph.getEdgeTarget(outgoingEdge);
				rightChild = getWeightedNode(nextvertice, graph);
				root = createSequenceTopNode(startService, rightChild, graph);

			}
			// Start with parallel node
			else if (graph.outDegreeOf("startNode") > 1) {
				// root = createParallelNode(this, outgoingEdgeList);
				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
				outgoingEdges.addAll(graph.outgoingEdgesOf("startNode"));

				// create startNode associated with all outgoing edges
				ServiceGPNode startService = new ServiceGPNode();
				startService.setSerName("startNode");

				rightChild = createParallelNode(outgoingEdges, graph);
				// root = createSequenceTopNode(startService, rightChild,
				// graph);
				root = createSequenceNode(startService, rightChild);

			}
		} else {
			// Begin by checking how many nodes are in the right child.
			GPNode rightChild;
			GPNode R = null;

			List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
			outgoingEdges.addAll(graph.outgoingEdgesOf(vertice));

			// Find the end node in the list, if it is contained there
			ServiceEdge outputEdge = null;
			for (ServiceEdge outgoingedge : outgoingEdges) {
				if (graph.getEdgeTarget(outgoingedge).equals("endNode")) {
					outputEdge = outgoingedge;
					// Create sequenceNode associated with endNode
					List<ServiceEdge> outgoingEdgeSet = new ArrayList<ServiceEdge>();
					outgoingEdgeSet.add(outputEdge);
					ServiceGPNode sgp = new ServiceGPNode();
					sgp.setSerName(vertice);
					ServiceGPNode endNode = new ServiceGPNode();
					endNode.setSerName("endNode");
					R = createSequenceNode(sgp, endNode);

					// Remove the output node from the children list

					outgoingEdges.remove(outputEdge);
					break;
				}
			}

			// If there is only one other child, create a sequence construct
			if (outgoingEdges.size() == 1) {
				rightChild = getWeightedNode(graph.getEdgeTarget(outgoingEdges.get(0)), graph);

				// Set<ServiceEdge> outgoingEdgeSet = new
				// HashSet<ServiceEdge>(outgoingEdges);
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setSerName(vertice);

				if (outputEdge != null) {
					GPNode L = createSequenceNode(sgp, rightChild);
					root = createParallelNode(R, L);
				} else {
					root = createSequenceNode(sgp, rightChild);
				}
			}

			// Else, create a new parallel construct wrapped in a sequence
			// construct
			else if (outgoingEdges.size() > 1) {
				rightChild = createParallelNode(outgoingEdges, graph);

				// Set<ServiceEdge> outgoingEdgeSet = new
				// HashSet<ServiceEdge>(outgoingEdges);

				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setSerName(vertice);
				if (outputEdge != null) {
					GPNode L = createSequenceNode(sgp, rightChild);
					root = createParallelNode(R, L);
				} else {
					root = createSequenceNode(sgp, rightChild);
				}
			}

		}

		return root;
	}

	/**
	 * Indirectly recursive method that transforms this GraphNode and all nodes
	 * that directly or indirectly receive its output into a tree representation
	 * considering semantic information that both startNodes and endNodes are
	 * included
	 *
	 * @return Tree root
	 */
	public GPNode toWeightedTree(String vertice, ServiceGraph graph) {
		GPNode root = null;
		if (vertice.equals("startNode")) {
			// Start with sequence
			// ServiceGPNode startService = new ServiceGPNode();
			// startService.setSerName("startNode");
			GPNode rightChild;

			if (graph.outDegreeOf("startNode") == 1) {
				/*
				 * If the next node points to the output, this is a
				 * single-service composition, so return a service node
				 */

				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
				outgoingEdges.addAll(graph.outgoingEdgesOf("startNode"));
				// create startNode associated with all outgoing edges
				ServiceGPNode startService = new ServiceGPNode(outgoingEdges);
				startService.setSerName("startNode");

				ServiceEdge outgoingEdge = outgoingEdges.get(0);
				String nextvertice = graph.getEdgeTarget(outgoingEdge);
				rightChild = getWeightedNode(nextvertice, graph);
				root = createSequenceTopNode(startService, rightChild, graph);

			}
			// Start with parallel node
			else if (graph.outDegreeOf("startNode") > 1) {
				// root = createParallelNode(this, outgoingEdgeList);
				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
				outgoingEdges.addAll(graph.outgoingEdgesOf("startNode"));

				// create startNode associated with all outgoing edges
				ServiceGPNode startService = new ServiceGPNode(outgoingEdges);
				startService.setSerName("startNode");

				rightChild = createParallelNode(outgoingEdges, graph);
				// root = createSequenceTopNode(startService, rightChild,
				// graph);
				root = createSequenceNode(startService, rightChild);

			}
		} else {
			// Begin by checking how many nodes are in the right child.
			GPNode rightChild;

			List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
			outgoingEdges.addAll(graph.outgoingEdgesOf(vertice));

			// Find the end node in the list, if it is contained there
			ServiceEdge outputEdge = null;
			for (ServiceEdge outgoingedge : outgoingEdges) {
				if (graph.getEdgeTarget(outgoingedge).equals("endNode")) {
					outputEdge = outgoingedge;
					// Create sequenceNode associated with endNode
					List<ServiceEdge> outgoingEdgeSet = new ArrayList<ServiceEdge>();
					outgoingEdgeSet.add(outputEdge);
					ServiceGPNode sgp = new ServiceGPNode(outgoingEdgeSet);
					ServiceGPNode endNode = new ServiceGPNode();
					endNode.setSerName("endNode");
					root = createSequenceNode(sgp, endNode);

					// Remove the output node from the children list

					outgoingEdges.remove(outputEdge);
					break;
				}
			}

			// If there is only one other child, create a sequence construct
			if (outgoingEdges.size() == 1) {
				rightChild = getWeightedNode(graph.getEdgeTarget(outgoingEdges.get(0)), graph);

				// Set<ServiceEdge> outgoingEdgeSet = new
				// HashSet<ServiceEdge>(outgoingEdges);
				ServiceGPNode sgp = new ServiceGPNode(outgoingEdges);
				sgp.setSerName(vertice);
				root = createSequenceNode(sgp, rightChild);
			}

			// Else if there are no children at all, return a new leaf node
			// else if (outgoingEdges.size() == 0) {
			// List<ServiceEdge> outgoingEdges2End = new
			// ArrayList<ServiceEdge>(outgoingEdges);
			// outgoingEdges2End.addAll(graph.getAllEdges(vertice, "endNode"));
			//
			// ServiceGPNode sgp = new ServiceGPNode(outgoingEdges2End);
			// sgp.setSerName(vertice);
			// ServiceGPNode endNode = new ServiceGPNode();
			// endNode.setSerName("endNode");
			// root = createSequenceNode(sgp, endNode);
			// }
			// Else, create a new parallel construct wrapped in a sequence
			// construct
			else if (outgoingEdges.size() > 1) {
				rightChild = createParallelNode(outgoingEdges, graph);

				// Set<ServiceEdge> outgoingEdgeSet = new
				// HashSet<ServiceEdge>(outgoingEdges);

				ServiceGPNode sgp = new ServiceGPNode(outgoingEdges);
				sgp.setSerName(vertice);
				root = createSequenceNode(sgp, rightChild);
			}

		}

		return root;
	}

	/**
	 * Indirectly recursive method that transforms this GraphNode and all nodes
	 * that directly or indirectly receive its output into a tree representation
	 * considering semantic information for mutation operation that only
	 * endNodes are included
	 *
	 * @return Tree root
	 */
	public GPNode toTree4Mutation(String vertice, ServiceGraph graph) {
		GPNode root = null;
		if (vertice.equals("startNode")) {
			// Start with sequence
			// ServiceGPNode startService = new ServiceGPNode();
			// startService.setSerName("startNode");
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
				root = getNode(nextvertice, graph);

			}
			// Start with parallel node
			else if (graph.outDegreeOf("startNode") > 1) {
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
			ServiceEdge outputEdge = null;
			for (ServiceEdge outgoingedge : outgoingEdges) {
				if (graph.getEdgeTarget(outgoingedge).equals("endNode")) {
					outputEdge = outgoingedge;
					// Create sequenceNode associated with endNode
					List<ServiceEdge> outgoingEdgeSet = new ArrayList<ServiceEdge>();
					outgoingEdgeSet.add(outputEdge);
					ServiceGPNode sgp = new ServiceGPNode(outgoingEdgeSet);
					ServiceGPNode endNode = new ServiceGPNode();
					endNode.setSerName("endNode");
					root = createSequenceNode(sgp, endNode);

					// Remove the output node from the children list

					outgoingEdges.remove(outputEdge);
					break;
				}
			}

			// If there is only one other child, create a sequence construct
			if (outgoingEdges.size() == 1) {
				rightChild = getWeightedNode(graph.getEdgeTarget(outgoingEdges.get(0)), graph);

				// Set<ServiceEdge> outgoingEdgeSet = new
				// HashSet<ServiceEdge>(outgoingEdges);
				ServiceGPNode sgp = new ServiceGPNode(outgoingEdges);
				sgp.setSerName(vertice);
				root = createSequenceNode(sgp, rightChild);
			}

			// Else if there are no children at all, return a new leaf node
			// else if (outgoingEdges.size() == 0) {
			// Set<ServiceEdge> outgoingEdgeSet = new
			// HashSet<ServiceEdge>(outgoingEdges);
			// ServiceGPNode sgp = new ServiceGPNode(outgoingEdgeSet);
			// sgp.setSerName(vertice);
			// ServiceGPNode endNode = new ServiceGPNode();
			// endNode.setSerName("endNode");
			// root = createSequenceNode(sgp, endNode);
			// }
			// Else, create a new parallel construct wrapped in a sequence
			// construct
			else if (outgoingEdges.size() > 1) {
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
			ServiceEdge outputEdge = null;
			for (ServiceEdge outgoingedge : outgoingEdges) {
				if (graph.getEdgeTarget(outgoingedge).equals("endNode")) {
					outputEdge = outgoingedge;
					// Remove the output node from the children list
					outgoingEdges.remove(outputEdge);
					break;
				}
			}

			// If there is only one other child, create a sequence construct
			if (outgoingEdges.size() == 1) {
				rightChild = getNode(graph.getEdgeTarget(outgoingEdges.get(0)), graph);
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
	private GPNode createParallelNode(List<ServiceEdge> outgoingEdges, ServiceGraph graph) {
		GPNode root = new ParallelGPNode();

		// Create subtrees for children
		int length = outgoingEdges.size();
		GPNode[] children = new GPNode[length];

		for (int i = 0; i < length; i++) {
			String nextVertice = graph.getEdgeTarget(outgoingEdges.get(i));
			// children[i] = getNode(nextVertice, graph);
			children[i] = getWeightedNode(nextVertice, graph);
			children[i].parent = root;
		}
		root.children = children;
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
	private GPNode createParallelNode(GPNode lChild, GPNode rChild) {
		GPNode root = new ParallelGPNode();

		// Create subtrees for children
		GPNode[] children = new GPNode[2];
		children[0].parent = lChild;
		children[1].parent = rChild;

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

		// Set<ServiceEdge> semanticEdgeList = new HashSet<ServiceEdge>();
		// semanticEdgeList.addAll(graph.edgeSet());
		// root.setSemanticEdges(semanticEdgeList);

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
			List<ServiceEdge> outgoingEdges2End = new ArrayList<ServiceEdge>();
			outgoingEdges2End.addAll(graph.getAllEdges(nextvertice, "endNode"));
			ServiceGPNode sgp = new ServiceGPNode(outgoingEdges2End);
			sgp.setSerName(nextvertice);
			ServiceGPNode endNode = new ServiceGPNode();
			endNode.setSerName("endNode");
			result = createSequenceNode(sgp, endNode);
		}
		// Otherwise, make next node's subtree the right child
		else
			result = toSemanticTree(nextvertice, graph);
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
		else {
			result = toTree(nextvertice, graph);
		}
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

}