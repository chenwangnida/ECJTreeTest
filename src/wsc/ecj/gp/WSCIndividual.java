package wsc.ecj.gp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.simple.SimpleFitness;
import ec.util.Parameter;
import wsc.graph.ParamterConn;
import wsc.graph.ServiceEdge;
import wsc.graph.ServiceGraph;

public class WSCIndividual extends GPIndividual {

	private static final long serialVersionUID = 1L;
	private static Set<String> targetSerIdSet = new HashSet<String>();

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

	// Replace the GPNodes and associated semantic edges
	public void replaceNode4Crossover(GPNode node, GPNode replacement) {
		// Perform replacement if neither node is not null
		if (node != null && replacement != null) {

			// replace is a service , selected is a functional node
			if ((replacement instanceof ServiceGPNode)
					&& ((node instanceof SequenceGPNode) || (node instanceof ParallelGPNode))) {

				GPNode sourceOfNode = getSourceGPNode(node);
				GPNode sourceOfReplacement = getSourceGPNode(replacement);

				if (((ServiceGPNode) sourceOfNode).getSemanticEdges().size() != ((ServiceGPNode) sourceOfReplacement)
						.getSemanticEdges().size()) {
					System.out.println("Case1 : orginal Sematic size"
							+ ((ServiceGPNode) sourceOfNode).getSemanticEdges().size() + "replaced Semantic Size"
							+ ((ServiceGPNode) sourceOfReplacement).getSemanticEdges().size());
				}
				// update all the serviceEdges of sourceOfNode with that of
				// sourceOfReplacement
				List<ServiceEdge> EdgeOfsourceOfReplacement = ((ServiceGPNode) sourceOfReplacement).getSemanticEdges();
				((ServiceGPNode) sourceOfNode).setSemanticEdges(EdgeOfsourceOfReplacement);

				replacement = (GPNode) replacement.clone();

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

				GPNode sourceOfNode = getSourceGPNode(node);
				GPNode sourceOfReplacement = getSourceGPNode(replacement);

				if (((ServiceGPNode) sourceOfNode).getSemanticEdges().size() != ((ServiceGPNode) sourceOfReplacement)
						.getSemanticEdges().size()) {
					System.out.println("Case2 : orginal Sematic size"
							+ ((ServiceGPNode) sourceOfNode).getSemanticEdges().size() + "replaced Semantic Size"
							+ ((ServiceGPNode) sourceOfReplacement).getSemanticEdges().size());
				}
				// update the ServiceEdge of sourceOfNode with that of
				// sourceOfReplacement
				List<ServiceEdge> EdgeOfsourceOfReplacement = ((ServiceGPNode) sourceOfReplacement).getSemanticEdges();
				List<ServiceEdge> EdgeOfsourceOfNode = ((ServiceGPNode) node).getSemanticEdges();

				for (ServiceEdge serEdge : EdgeOfsourceOfNode) {
					if (serEdge.getSource().toString().equals(((ServiceGPNode) sourceOfNode).getSerName())) {
						EdgeOfsourceOfNode.remove(serEdge);
						break;
					}
				}
				EdgeOfsourceOfNode.addAll(EdgeOfsourceOfReplacement);
				((ServiceGPNode) sourceOfNode).setSemanticEdges(EdgeOfsourceOfNode);

				replacement = (GPNode) replacement.clone();

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

			} else {

				// two service nodes crossover or two operation nodes crossover
				// clone replacement that would not clone the parents, which
				// is
				// wrong
				// replacement = (GPNode) replacement.clone();

				// SourceNode of selected Node obtained
				// System.out.println(node.toString());
				GPNode sourceOfNode = getSourceGPNode(node);
				// SourceNode of replaced Node obtained
				GPNode sourceOfReplacement = getSourceGPNode(replacement);

				replacement = (GPNode) replacement.clone();

				// update the ServiceEdge of sourceOfNode with that of
				// sourceOfReplacement
				if (((ServiceGPNode) sourceOfNode).getSemanticEdges().size() != ((ServiceGPNode) sourceOfReplacement)
						.getSemanticEdges().size()) {
					System.out.println("Case3 : orginal Sematic size"
							+ ((ServiceGPNode) sourceOfNode).getSemanticEdges().size() + "replaced Semantic Size"
							+ ((ServiceGPNode) sourceOfReplacement).getSemanticEdges().size());
				}
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

	// Replace the GPNodes and associated semantic edges
	// public void replaceNode4Crossoverdefualt(GPNode node, GPNode replacement)
	// {
	// // Perform replacement if neither node is not null
	// if (node != null && replacement != null) {
	//
	// // clone replacement that would not clone the parents, which is
	// // wrong
	// // replacement = (GPNode) replacement.clone();
	//
	// // SourceNode of selected Node obtained
	// // System.out.println(node.toString());
	// GPNode sourceOfNode = getSourceGPNode(node);
	// // SourceNode of replaced Node obtained
	// GPNode sourceOfReplacement = getSourceGPNode(replacement);
	//
	// replacement = (GPNode) replacement.clone();
	//
	// // update the ServiceEdge of sourceOfNode with that of
	// // sourceOfReplacement
	//
	// Set<ServiceEdge> EdgeOfsourceOfReplacement = ((ServiceGPNode)
	// sourceOfReplacement).getSemanticEdges();
	// ((ServiceGPNode)
	// sourceOfNode).setSemanticEdges(EdgeOfsourceOfReplacement);
	//
	// // GPNode parentNode = (GPNode) node.parent;
	// // if (parentNode == null) {
	// // the selected node is the topNode in the tree
	// // super.trees[0].child = replacement;
	// // } else {
	//
	// GPNode parentNode = (GPNode) node.parent;
	//
	// replacement.parent = node.parent;
	// for (int i = 0; i < parentNode.children.length; i++) {
	// if (parentNode.children[i] == node) {
	// parentNode.children[i] = replacement;
	// // wonder whether to break while considering the
	// // redundant nodes in the tree transfered from the graph
	// break;
	// }
	// }
	//
	// }
	// }

	// private GPNode getSourceGPNode(GPNode node) {
	//
	// GPNode sourceGPNode = null;
	// GPNode parentNode = (GPNode) node.parent;
	// GPNode pOperatorNode = (GPNode) parentNode.parent;
	//
	// System.out.println("selected node for finding source node"+node);
	//
	// GPNode[] pOperatorNodeChild = pOperatorNode.children;
	//
	// for (GPNode ppOpChild : pOperatorNodeChild) {
	// if (ppOpChild instanceof ServiceGPNode) {
	// sourceGPNode = ppOpChild;
	// }
	// }
	//
	// if (sourceGPNode == null) {
	// GPNode ppOperatorNode = (GPNode) pOperatorNode.parent;
	// GPNode[] ppOpratorNodeChild = ppOperatorNode.children;
	// for (GPNode ppOpChild : ppOpratorNodeChild) {
	// if (ppOpChild instanceof ServiceGPNode) {
	// sourceGPNode = ppOpChild;
	// }
	// }
	// }
	//
	// if (sourceGPNode == null)
	// {
	// System.out.println("Wrong SourceNode of selected Node obttained under
	// crossover");
	// }
	// return sourceGPNode;
	// }

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

			// mutation the service dependency on sourceNode of selected node

			mutation4Weights(sourceOfNode, InComingEdgeOfReplaceNode);

			// Mutation the node information

			// mutate on service node
			if (node instanceof ServiceGPNode) {
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
	 * ParameterConn update and Weights aggregation for mutation process
	 * 
	 */

	private void mutation4Weights(GPNode sourceOfNode, List<ServiceEdge> InComingEdgeOfReplaceNode) {

		List<ServiceEdge> serEdgeList = ((ServiceGPNode) sourceOfNode).getSemanticEdges();
		String sourceOfnodeName = ((ServiceGPNode) sourceOfNode).getSerName();

		// Case one : replace all the semantic of sourceNode
		if (serEdgeList.size() == 1) {
			// obtain overall ParamterConn to replace the that of sourceNode
			for (ServiceEdge edgeOfReplaceNode : InComingEdgeOfReplaceNode) {
				for (ParamterConn p : edgeOfReplaceNode.getpConnList()) {
					p.setSourceServiceID(sourceOfnodeName);
				}
			}
			((ServiceGPNode) sourceOfNode).setSemanticEdges(InComingEdgeOfReplaceNode);

		} else {
			// Case two : replace part of the semantic of sourceNode
			// create List for storing all ParameterConn from sourceNode of
			// selectedNode
			List<ParamterConn> undispachedParaConnList = new ArrayList<ParamterConn>();
			for (ServiceEdge edgeOfNode : serEdgeList) {
				if (edgeOfNode.getTargetService() != sourceOfnodeName) {
					for (ParamterConn p : edgeOfNode.getpConnList()) {
						undispachedParaConnList.add(p);
					}
				}
			}

			// add ParamterConn from source of replaceNode
			for (ServiceEdge edgeOfnode : InComingEdgeOfReplaceNode) {
				undispachedParaConnList.addAll(edgeOfnode.getpConnList());
			}

			/*******
			 * Weights aggregation
			 * 
			 */
			// how many sourceService are connected
			targetSerIdSet.clear();
			double summt = 0.00;
			double sumdst = 0.00;
			for (ParamterConn p : undispachedParaConnList) {
				String targetSerID = p.getTargetServiceID();
				targetSerIdSet.add(targetSerID);
			}
			List<ServiceEdge> updatedSerEdgeList = new ArrayList<ServiceEdge>();
			// Edge are needed for each sourceService
			for (String targetSerID : targetSerIdSet) {
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
