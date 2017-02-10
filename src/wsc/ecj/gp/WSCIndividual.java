package wsc.ecj.gp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;

import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.simple.SimpleFitness;
import ec.util.Parameter;
import wsc.graph.ParamterConn;
import wsc.graph.ServiceEdge;
import wsc.graph.ServiceGraph;
import wsc.graph.ServiceInput;

public class WSCIndividual extends GPIndividual {

	private static final long serialVersionUID = 1L;
	private static Set<String> targetSerIdSet = new HashSet<String>();
	private static Set<String> originalTargetSerIdSet = new HashSet<String>();

	public WSCIndividual() {
		super();
		super.fitness = new SimpleFitness();
		super.species = new WSCSpecies();
	}

	public WSCIndividual(GPNode root) {
		super();
		super.fitness = new SimpleFitness();
		super.species = new WSCSpecies();
		super.trees = new GPTree[1];
		GPTree t = new GPTree();
		super.trees[0] = t;/** the root GPNode in the GPTree */
		t.child = root;
	}

	public WSCIndividual(GPNode root, ServiceGraph graph) {
		super();
		super.fitness = new SimpleFitness();
		super.species = new WSCSpecies();
		super.trees = new GPTree[1];
		GPTree t = new GPTree();
		super.trees[0] = t;/** the root GPNode in the GPTree */
		t.child = root;
	}

	@Override
	public Parameter defaultBase() {
		return new Parameter("wscindividual");
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof WSCIndividual) {
			return toString().equals(other.toString());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("digraph tree { ");
		builder.append(trees[0].child.toString());
		builder.append("}");
		return builder.toString();
	}

	@Override
	public WSCIndividual clone() {
		WSCIndividual wsci = new WSCIndividual((GPNode) super.trees[0].child.clone());
		wsci.fitness = (SimpleFitness) fitness.clone();
		wsci.species = species;
		return wsci;
	}

	// Get FiltedTreeNodes not including startNodes and endNodes
	public List<GPNode> getFiltedTreeNodes() {
		List<GPNode> allNodes = new ArrayList<GPNode>();
		AddFiltedChildNodes(trees[0].child, allNodes);

		List<GPNode> removedNodeList = new ArrayList<GPNode>();
		for (int i = 0; i < allNodes.size(); i++) {
			GPNode filteredChild = allNodes.get(i);
			if (filteredChild instanceof ServiceGPNode) {
				ServiceGPNode sgp = (ServiceGPNode) filteredChild;
				if (sgp.getSerName().equals("startNode")) {
					// initial variable rootNode
					removedNodeList.add((GPNode) sgp.parent);
					// remove startNode
					removedNodeList.add(allNodes.get(i));
				}
				if (sgp.getSerName().equals("endNode")) {
					// initial variable endParentNodeList
					removedNodeList.add((GPNode) sgp.parent);
					// remove endNode
					removedNodeList.add(allNodes.get(i));
				}
			}
		}

		allNodes.removeAll(removedNodeList);

		return allNodes;
	}

	public List<GPNode> getOnlyServiceGPNodes(GPNode replacement) {
		List<GPNode> allNodes = new ArrayList<GPNode>();
		AddChildNodes(replacement, allNodes);

		List<GPNode> removedNodeList = new ArrayList<GPNode>();
		for (int i = 0; i < allNodes.size(); i++) {
			GPNode filteredChild = allNodes.get(i);
			if (filteredChild instanceof ServiceGPNode) {
				ServiceGPNode sgp = (ServiceGPNode) filteredChild;
				if (sgp.getSerName().equals("startNode")) {
					// initial variable rootNode
					removedNodeList.add((GPNode) sgp.parent);
					// remove startNode
					removedNodeList.add(allNodes.get(i));
				}
				if (sgp.getSerName().equals("endNode")) {
					// initial variable endParentNodeList
					removedNodeList.add((GPNode) sgp.parent);
					// remove endNode
					removedNodeList.add(allNodes.get(i));
				}
			}
			if (filteredChild instanceof SequenceGPNode) {
				removedNodeList.add(filteredChild);
			}
			if (filteredChild instanceof ParallelGPNode) {
				removedNodeList.add(filteredChild);
			}
		}

		allNodes.removeAll(removedNodeList);

		return allNodes;
	}

	public List<GPNode> AddFiltedChildNodes(GPNode gpChild, List<GPNode> allNodes) {

		GPNode current = gpChild;
		allNodes.add(current);
		if (current.children != null) {
			for (GPNode child : current.children)
				AddChildNodes(child, allNodes);
		}
		return allNodes;

	}

	// Get AllTreeNodes

	public List<GPNode> getAllTreeNodes() {
		List<GPNode> allNodes = new ArrayList<GPNode>();
		AddChildNodes(trees[0].child, allNodes);

		return allNodes;
	}

	// Get All Nodes from GPNode

	public List<GPNode> getAllTreeNodes(GPNode gpNode) {
		List<GPNode> allNodes = new ArrayList<GPNode>();
		// AddChildNodes(trees[0].child, allNodes);
		AddChildNodes(gpNode, allNodes);

		return allNodes;
	}

	public List<GPNode> AddChildNodes(GPNode gpChild, List<GPNode> allNodes) {

		GPNode current = gpChild;
		allNodes.add(current);
		if (current.children != null) {
			for (GPNode child : current.children)
				AddChildNodes(child, allNodes);
		}
		return allNodes;

	}

	private List<ServiceEdge> getIncomingEdgeOfGPNode(GPNode replacement, GPNode sourceOfReplacement) {
		List<ServiceEdge> inComingEdgeOfReplaceNode = new ArrayList<ServiceEdge>();

		// obtain the inComingEdges Of Replacement
		InOutNode ioNode = (InOutNode) replacement;
		List<ServiceInput> requireInstList = ioNode.getInputs();
		List<String> requrieInstStrList = new ArrayList<String>();
		for (ServiceInput serInput : requireInstList) {
			requrieInstStrList.add(serInput.getInput());
		}

		for (ServiceEdge edgeOfNode : ((ServiceGPNode) sourceOfReplacement).getSemanticEdges()) {
			for (ParamterConn p : edgeOfNode.getpConnList()) {
				String existInput = p.getOutputrequ();
				if (requrieInstStrList.contains(existInput)) {
					if (!inComingEdgeOfReplaceNode.contains(edgeOfNode)) {
						inComingEdgeOfReplaceNode.add(edgeOfNode);
					}
					break;
				}

			}
		}

		//

		if (inComingEdgeOfReplaceNode.size() == 0) {
			System.err.println("InComingEdgeOfReplaceNode" + inComingEdgeOfReplaceNode.size());
		}
		return inComingEdgeOfReplaceNode;

	}

	// Replace the GPNodes and associated semantic edges
	public void replaceNode4Crossover(GPNode node, GPNode replacement, BiMap<String, String> inst1Toinst2) {
		// Perform replacement if neither node is not null
		if (node != null && replacement != null) {

			GPNode sourceOfNode = getSourceGPNode(node);
			GPNode sourceOfReplacement = getSourceGPNode(replacement);

			// obtain the inComingEdges of replaceNode
			List<ServiceEdge> inComingEdgeOfReplaceNode = getIncomingEdgeOfGPNode(replacement, sourceOfReplacement);

			// update the outgoingEdges of source node of selected node
			updateWeightsOfSourceNode(sourceOfNode, node, inComingEdgeOfReplaceNode);

			// selected is a functional node, and replace is a service
			if ((replacement instanceof ServiceGPNode)
					&& ((node instanceof SequenceGPNode) || (node instanceof ParallelGPNode))) {

				replacement = (GPNode) replacement.clone();

				// update the target node of replacement node
				for (ServiceEdge edge2EndNode : ((ServiceGPNode) replacement).getSemanticEdges()) {
					edge2EndNode.setTargetService("endNode");
					for (ParamterConn pnn : edge2EndNode.getpConnList()) {
						pnn.setTargetServiceID("endNode");
					}
				}

				// Create sequenceNode associated with endNode
				ServiceGPNode endNode = new ServiceGPNode();
				endNode.setSerName("endNode");
				GPNode newReplacement = createSequenceNode(replacement, endNode);

				// swap
				GPNode parentNode = (GPNode) node.parent;

				// replacement.parent = node.parent;
				newReplacement.parent = node.parent;

				for (int i = 0; i < parentNode.children.length; i++) {
					if (parentNode.children[i] == node) {
						parentNode.children[i] = newReplacement;
						// wonder whether to break while considering the
						// redundant nodes in the tree transfered from the
						// graph
						break;
					}
				}

			} else if ((node instanceof ServiceGPNode)
					&& ((replacement instanceof SequenceGPNode) || (node instanceof ParallelGPNode))) {
				replacement = (GPNode) replacement.clone();

				// update the target node of replacement node

				updateWeightsOfTargetNodes4Crossover(node, replacement, inst1Toinst2);

				// replacement is a functional node , selected is a service
				// node

				GPNode pNode = (GPNode) node.parent;
				GPNode ppNode = (GPNode) pNode.parent;

				// obtain the appedixNode to tailed as the deleted endNode
				// in
				// replacement
				GPNode appedixNode = null;
				// GPNode endNode = null;
				List<GPNode> endNodeList = new ArrayList<GPNode>();
				GPNode[] appedix = pNode.children;
				for (GPNode aNode : appedix) {
					if (aNode != node) {
						appedixNode = aNode;
					}
				}
				// find the endNode in replacement
				List<GPNode> allNodeofReplacement = this.getAllTreeNodes(replacement);
				for (GPNode gpn : allNodeofReplacement) {
					if (gpn instanceof ServiceGPNode) {
						if (((ServiceGPNode) gpn).getSerName().equals("endNode")) {
							// endNode = gpn;
							endNodeList.add(gpn);
							// replacement.cloneReplacingAtomic(appedixNode,
							// gpn);

						}
					}
				}

				// replace the endNode with appedixNode
				// replaceNode(endNode, appedixNode);
				for (GPNode endNode : endNodeList) {

					GPNode parentEndNode = (GPNode) endNode.parent;
					appedixNode.parent = endNode.parent;
					for (int i = 0; i < parentEndNode.children.length; i++) {
						if (parentEndNode.children[i] == endNode) {
							parentEndNode.children[i] = appedixNode;
							// wonder whether to break while considering the
							// redundant nodes in the tree transfered from
							// the
							// graph
							break;
						}
					}

				}

				// replace replacement in the graph
				replacement.parent = pNode.parent;
				for (int i = 0; i < ppNode.children.length; i++) {
					if (ppNode.children[i] == pNode) {
						ppNode.children[i] = replacement;
						// wonder whether to break while considering the
						// redundant nodes in the tree transfered from the
						// graph
						break;
					}
				}

			} else if ((node instanceof ServiceGPNode) && (replacement instanceof ServiceGPNode)) {

				updateWeightsOfTargetNodes4Crossover(node, replacement, inst1Toinst2);

				replacement = (GPNode) replacement.clone();

				List<ServiceEdge> EdgeOfsourceOfReplacement = ((ServiceGPNode) sourceOfReplacement).getSemanticEdges();
				((ServiceGPNode) sourceOfNode).setSemanticEdges(EdgeOfsourceOfReplacement);

				GPNode parentNode = (GPNode) node.parent;

				replacement.parent = node.parent;
				for (int i = 0; i < parentNode.children.length; i++) {
					if (parentNode.children[i] == node) {
						parentNode.children[i] = replacement;
						// wonder whether to break while considering the
						// redundant nodes in the tree transfered from the
						// graph
						break;
					}
				}

			} else {

				replacement = (GPNode) replacement.clone();

				// update the ServiceEdge of sourceOfNode with that of
				// sourceOfReplacement
				// if (((ServiceGPNode) sourceOfNode).getSemanticEdges().size()
				// != ((ServiceGPNode) sourceOfReplacement)
				// .getSemanticEdges().size()) {
				// System.out.println("Case3 : orginal Sematic size"
				// + ((ServiceGPNode) sourceOfNode).getSemanticEdges().size() +
				// "replaced Semantic Size"
				// + ((ServiceGPNode)
				// sourceOfReplacement).getSemanticEdges().size());
				// }
				List<ServiceEdge> EdgeOfsourceOfReplacement = ((ServiceGPNode) sourceOfReplacement).getSemanticEdges();
				((ServiceGPNode) sourceOfNode).setSemanticEdges(EdgeOfsourceOfReplacement);

				// GPNode parentNode = (GPNode) node.parent;
				// if (parentNode == null) {
				// the selected node is the topNode in the tree
				// super.trees[0].child = replacement;
				// } else {

				GPNode parentNode = (GPNode) node.parent;

				replacement.parent = node.parent;
				for (int i = 0; i < parentNode.children.length; i++) {
					if (parentNode.children[i] == node) {
						parentNode.children[i] = replacement;
						// wonder whether to break while considering the
						// redundant nodes in the tree transfered from the
						// graph
						break;
					}
				}

			}

		}

	}

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

	private GPNode isSourceGPNode(GPNode parentNode, GPNode sourceGPNode) {

		for (GPNode sourceChild : parentNode.children) {
			if (sourceChild instanceof ServiceGPNode) {
				sourceGPNode = sourceChild;
				return sourceGPNode;
			}
		}
		return isSourceGPNode((GPNode) parentNode.parent, sourceGPNode);
	}

	private GPNode getSourceGPNode(GPNode node) {
		// System.out.println(node.toString());
		GPNode sourceGPNode = null;
		GPNode parentNode = (GPNode) node.parent;

		if (node instanceof ServiceGPNode) {
			GPNode pparentNode = (GPNode) parentNode.parent;
			sourceGPNode = isSourceGPNode(pparentNode, sourceGPNode);

		} else {
			sourceGPNode = isSourceGPNode(parentNode, sourceGPNode);
		}

		if (sourceGPNode == null) {

			System.out.println("Wrong SourceNode of selected Node obttained under crossover" + node);
		}
		return sourceGPNode;
	}

	public void replaceNode4Mutation(GPNode node, GPNode replacement) {
		// Perform replacement if neither node is not null
		if (node != null && replacement != null) {
			// clone replacement
			// replacement = (GPNode) replacement.clone();

			GPNode[] replacementList = replacement.children.clone();
			List<ServiceEdge> InComingEdgeOfReplaceNode = null;

			// obain semantic Edge and replace part of replaceNode
			for (GPNode gpNode : replacementList) {
				if (gpNode instanceof SequenceGPNode) {
					// update the replacement without including the startNode
					// and associated sequenceNode
					replacement = gpNode;
				}

				if (gpNode instanceof ServiceGPNode) {
					// obtain inComingEdge of StartNode from graph4Mutation
					InComingEdgeOfReplaceNode = ((ServiceGPNode) gpNode).getSemanticEdges();
				}
			}

			GPNode sourceOfNode = getSourceGPNode(node);

			// update the outgoing edges of source node Of node regardless node
			// is service node or functional node

			updateWeightsOfSourceNode(sourceOfNode, node, InComingEdgeOfReplaceNode);

			// mutate on service node
			if (node instanceof ServiceGPNode) {

				// mutation the service dependency on several targetNode of
				// selected
				// nodes only considered in case of mutation on service node
				updateWeightsOfTargetNodes(replacement);

				GPNode pNode = (GPNode) node.parent;
				GPNode ppNode = (GPNode) pNode.parent;

				// obtain the appedixNode to tailed as the deleted endNode in
				// replacement
				GPNode appedixNode = null;
				// GPNode endNode = null;
				List<GPNode> endNodeList = new ArrayList<GPNode>();
				GPNode[] appedix = pNode.children;
				for (GPNode aNode : appedix) {
					if (aNode != node) {
						appedixNode = aNode;
					}
				}
				// find the endNode in replacement
				List<GPNode> allNodeofReplacement = this.getAllTreeNodes(replacement);
				for (GPNode gpn : allNodeofReplacement) {
					if (gpn instanceof ServiceGPNode) {
						if (((ServiceGPNode) gpn).getSerName().equals("endNode")) {
							// endNode = gpn;
							endNodeList.add(gpn);
							// replacement.cloneReplacingAtomic(appedixNode,
							// gpn);

						}
					}
				}

				// replace the endNode with appedixNode
				// replaceNode(endNode, appedixNode);
				for (GPNode endNode : endNodeList) {

					GPNode parentEndNode = (GPNode) endNode.parent;
					appedixNode.parent = endNode.parent;
					for (int i = 0; i < parentEndNode.children.length; i++) {
						if (parentEndNode.children[i] == endNode) {
							parentEndNode.children[i] = appedixNode;
							// wonder whether to break while considering the
							// redundant nodes in the tree transfered from the
							// graph
							break;
						}
					}

				}

				// replace replacement in the graph
				replacement.parent = pNode.parent;
				for (int i = 0; i < ppNode.children.length; i++) {
					if (ppNode.children[i] == pNode) {
						ppNode.children[i] = replacement;
						// wonder whether to break while considering the
						// redundant nodes in the tree transfered from the graph
						break;
					}
				}

			} else {

				// mutate on functional nodes
				// there is no need for updating the target node of selected
				// node
				GPNode parentNode = (GPNode) node.parent;
				replacement.parent = node.parent;
				for (int i = 0; i < parentNode.children.length; i++) {
					if (parentNode.children[i] == node) {
						parentNode.children[i] = replacement;
						// wonder whether to break while considering the
						// redundant nodes in the tree transfered from the graph
						break;
					}
				}
			}

		}
	}

	/*******
	 * ParameterConn update and Weights aggregation for target nodes during
	 * mutation process Caution: it is only considered for mutation on the
	 * service node
	 */

	private void updateWeightsOfTargetNodes(GPNode replacement) {
		// get edges that target endNode
		Set<GPNode> sourceOfEndNodeSet = new HashSet<GPNode>();
		List<ParamterConn> undispachedParaConnList = new ArrayList<ParamterConn>();

		List<GPNode> allReplacementNode = this.getOnlyServiceGPNodes(replacement);
		for (GPNode replacementNode : allReplacementNode) {
			if (replacementNode instanceof ServiceGPNode) {
				for (ServiceEdge serEdge : ((ServiceGPNode) replacementNode).getSemanticEdges()) {
					if (serEdge.getTargetService() == "endNode" || serEdge.getTargetService().equals("endNode")) {
						for (ParamterConn pConn : serEdge.getpConnList()) {
							// get all the parametersConn
							sourceOfEndNodeSet.add(replacementNode);
							undispachedParaConnList.add(pConn);
						}

					}

				}
			}
		}
		// a map from sourceofEndnode name to its related parameter connections
		Map<String, List<ParamterConn>> map = new HashMap<String, List<ParamterConn>>();
		// a map from sourceofEndnode name to its GPNode
		Map<String, GPNode> gpNodeMapfromName = new HashMap<String, GPNode>();

		// initialize map with list of parameter
		for (GPNode sourceOfEndNode : sourceOfEndNodeSet) {
			String sourceOfEndNodeName = ((ServiceGPNode) sourceOfEndNode).getSerName();
			gpNodeMapfromName.put(sourceOfEndNodeName, sourceOfEndNode);
			List<ParamterConn> groupedParaConnList = new ArrayList<ParamterConn>();
			map.put(sourceOfEndNodeName, groupedParaConnList);
		}

		for (ParamterConn pc : undispachedParaConnList) {
			map.get(pc.getSourceServiceID()).add(pc);
		}

		for (String sourceOfEndNodeName : map.keySet()) {
			GPNode sourceOfEndNode = gpNodeMapfromName.get(sourceOfEndNodeName);
			aggregsteWeigthsOfNode(sourceOfEndNode, map.get(sourceOfEndNodeName));
		}

	}

	private void aggregsteWeigthsOfNode(GPNode sourceOfEndNode, List<ParamterConn> undispachedParaConnList) {
		String sourceOfnodeName = ((ServiceGPNode) sourceOfEndNode).getSerName();

		// how many sourceService are connected
		originalTargetSerIdSet.clear();
		double summt = 0.00;
		double sumdst = 0.00;
		for (ParamterConn p : undispachedParaConnList) {
			String originalTargetSerID = p.getOriginalTargetServiceId();
			originalTargetSerIdSet.add(originalTargetSerID);
		}
		List<ServiceEdge> updatedSerEdgeList = new ArrayList<ServiceEdge>();
		// Edge are needed for each sourceService
		for (String targetSerID : originalTargetSerIdSet) {
			ServiceEdge serEdge = new ServiceEdge(0, 0);
			serEdge.setSourceService(sourceOfnodeName);
			serEdge.setTargetService(targetSerID);
			// how many parameter connection needed for each Edge
			for (ParamterConn p : undispachedParaConnList) {
				if (p.getTargetServiceID().equals(targetSerID)) {
					serEdge.getpConnList().add(p);
				}
			}
			// add Edge to a EdgeList to calcute each edge aggregation
			// and build edge for graph
			updatedSerEdgeList.add(serEdge);
		}

		for (ServiceEdge edge : updatedSerEdgeList) {
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
		}
		// set it for the replaced nodes or node
		((ServiceGPNode) sourceOfEndNode).setSemanticEdges(updatedSerEdgeList);
	}

	/*******
	 * ParameterConn update and Weights aggregation for target nodes during
	 * mutation process Caution: it is only considered for mutation on the
	 * service node
	 */

	private void updateWeightsOfTargetNodes4Crossover(GPNode node, GPNode replacement,
			BiMap<String, String> inst1Toinst2) {
		// get edges that target endNode
		Set<GPNode> sourceOfEndNodeSet = new HashSet<GPNode>();
		List<ParamterConn> undispachedParaConnList = new ArrayList<ParamterConn>();

		if (replacement instanceof ServiceGPNode) {
			sourceOfEndNodeSet.add(replacement);
			for (ServiceEdge serEdge : ((ServiceGPNode) replacement).getSemanticEdges()) {
				for (ParamterConn pConn : serEdge.getpConnList()) {
					undispachedParaConnList.add(pConn);
				}
			}
		} else {
			List<GPNode> allReplacementNode = this.getOnlyServiceGPNodes(replacement);
			for (GPNode replacementNode : allReplacementNode) {
				if (replacementNode instanceof ServiceGPNode) {
					for (ServiceEdge serEdge : ((ServiceGPNode) replacementNode).getSemanticEdges()) {
						if (serEdge.getTargetService() == "endNode" || serEdge.getTargetService().equals("endNode")) {
							for (ParamterConn pConn : serEdge.getpConnList()) {
								// get all the parametersConn
								sourceOfEndNodeSet.add(replacementNode);
								undispachedParaConnList.add(pConn);
							}

						}

					}
				}
			}

		}

		// a map from sourceofEndnode name to its related parameter connections
		Map<String, List<ParamterConn>> map = new HashMap<String, List<ParamterConn>>();
		// a map from sourceofEndnode name to its GPNode
		Map<String, GPNode> gpNodeMapfromName = new HashMap<String, GPNode>();

		// initialize map with list of parameter
		for (GPNode sourceOfEndNode : sourceOfEndNodeSet) {
			String sourceOfEndNodeName = ((ServiceGPNode) sourceOfEndNode).getSerName();
			gpNodeMapfromName.put(sourceOfEndNodeName, sourceOfEndNode);
			List<ParamterConn> groupedParaConnList = new ArrayList<ParamterConn>();
			map.put(sourceOfEndNodeName, groupedParaConnList);
		}

		for (ParamterConn pc : undispachedParaConnList) {
			map.get(pc.getSourceServiceID()).add(pc);
		}

		for (String sourceOfEndNodeName : map.keySet()) {
			GPNode sourceOfEndNode = gpNodeMapfromName.get(sourceOfEndNodeName);
			aggregsteWeigthsOfNode4Crossover(node, sourceOfEndNode, map.get(sourceOfEndNodeName), inst1Toinst2);
		}

	}

	private void aggregsteWeigthsOfNode4Crossover(GPNode node, GPNode sourceOfEndNode,
			List<ParamterConn> undispachedParaConnList, BiMap<String, String> inst1Toinst2) {
		String sourceOfnodeName = ((ServiceGPNode) sourceOfEndNode).getSerName();

		// how many sourceService are connected
		originalTargetSerIdSet.clear();
		double summt = 0.00;
		double sumdst = 0.00;

		Map<String, String> instToServMap = new HashMap<String, String>();

		for (ServiceEdge e : ((ServiceGPNode) node).getSemanticEdges()) {
			for (ParamterConn pc : e.getpConnList()) {
				instToServMap.put(pc.getOutputInst(), pc.getTargetServiceID());
			}
		}

		for (ParamterConn p : undispachedParaConnList) {
			// obtain mapped instance from selected node, and get
			// originalTargetSerId

			String originalTargetSerId = null;

			if (inst1Toinst2.get(p.getOutputInst()) != null) {
				originalTargetSerId = instToServMap.get(inst1Toinst2.get(p.getOutputInst()));
				p.setTargetServiceID(originalTargetSerId);

			} else {
				originalTargetSerId = instToServMap.get(inst1Toinst2.inverse().get(p.getOutputInst()));
				p.setTargetServiceID(originalTargetSerId);

			}

			if(originalTargetSerId!=null){
			originalTargetSerIdSet.add(originalTargetSerId);
			}

		}

		List<ServiceEdge> updatedSerEdgeList = new ArrayList<ServiceEdge>();
		// Edge are needed for each sourceService
		for (String targetSerID : originalTargetSerIdSet) {
			ServiceEdge serEdge = new ServiceEdge(0, 0);
			serEdge.setSourceService(sourceOfnodeName);
			serEdge.setTargetService(targetSerID);
			// how many parameter connection needed for each Edge
			for (ParamterConn p : undispachedParaConnList) {
				if (p.getTargetServiceID().equals(targetSerID)) {
					serEdge.getpConnList().add(p);
				}
			}
			// add Edge to a EdgeList to calcute each edge aggregation
			// and build edge for graph
			updatedSerEdgeList.add(serEdge);
		}

		for (ServiceEdge edge : updatedSerEdgeList) {
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
		}
		// set it for the replaced nodes or node
		((ServiceGPNode) sourceOfEndNode).setSemanticEdges(updatedSerEdgeList);
	}

	private void updateWeightsOfSourceNode(GPNode sourceOfNode, GPNode node,
			List<ServiceEdge> InComingEdgeOfReplaceNode) {

		List<ServiceEdge> serEdgeList = ((ServiceGPNode) sourceOfNode).getSemanticEdges();
		String sourceOfnodeName = ((ServiceGPNode) sourceOfNode).getSerName();

		// Case one : replace all the semantic of sourceNode
		if (serEdgeList.size() == 1) {
			// update sourceServiceid and replace directly with the that of
			// sourceNode of replacement
			for (ServiceEdge edgeOfReplaceNode : InComingEdgeOfReplaceNode) {
				edgeOfReplaceNode.setSourceService(sourceOfnodeName);
				for (ParamterConn p : edgeOfReplaceNode.getpConnList()) {
					p.setSourceServiceID(sourceOfnodeName);
				}
			}
			((ServiceGPNode) sourceOfNode).setSemanticEdges(InComingEdgeOfReplaceNode);

		} else {
			// Case two : replace part of the semantic of sourceNode
			// create List for storing all ParameterConn from sourceNode of
			// selectedNode

			List<ServiceEdge> updatedSerEdgeList = new ArrayList<ServiceEdge>();

			// update sourceServiceid add parts from source node of replacement
			// node

			for (ServiceEdge edgeOfReplaceNode : InComingEdgeOfReplaceNode) {
				edgeOfReplaceNode.setSourceService(sourceOfnodeName);
				for (ParamterConn p : edgeOfReplaceNode.getpConnList()) {
					p.setSourceServiceID(sourceOfnodeName);
				}
			}

			updatedSerEdgeList.addAll(InComingEdgeOfReplaceNode);

			// add parts from source node of selected node if no matter it is a
			// service node or functional node

			InOutNode ioNode = (InOutNode) node;
			List<ServiceInput> requireInstList = ioNode.getInputs();
			List<String> requrieInstStrList = new ArrayList<String>();
			for (ServiceInput serInput : requireInstList) {
				requrieInstStrList.add(serInput.getInput());
			}

			for (ServiceEdge edgeOfNode : serEdgeList) {
				for (ParamterConn p : edgeOfNode.getpConnList()) {
					String existInput = p.getOutputrequ();
					if (!requrieInstStrList.contains(existInput)) {
						updatedSerEdgeList.add(edgeOfNode);
						break;
					}
				}
			}

			((ServiceGPNode) sourceOfNode).setSemanticEdges(updatedSerEdgeList);
		}

	}

	private GPNode replaceNode(GPNode endNode, GPNode appedixNode) {

		if (endNode != null && appedixNode != null) {
			// clone replacement
			appedixNode = (GPNode) appedixNode.clone();

			GPNode parentNode = (GPNode) endNode.parent;

			appedixNode.parent = endNode.parent;
			for (int i = 0; i < parentNode.children.length; i++) {
				if (parentNode.children[i] == endNode) {
					parentNode.children[i] = appedixNode;
					// wonder whether to break while considering the
					// redundant nodes in the tree transfered from the graph
					break;
				}
			}
		}
		return endNode;
	}
}
