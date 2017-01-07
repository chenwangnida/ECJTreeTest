package wsc.ecj.gp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.simple.SimpleFitness;
import ec.util.Parameter;
import wsc.graph.ServiceEdge;
import wsc.graph.ServiceGraph;
import wsc.graph.ServiceOutput;

public class WSCIndividual extends GPIndividual {

	private static final long serialVersionUID = 1L;

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
			// clone replacement
			replacement = (GPNode) replacement.clone();

			// SourceNode of selected Node obtained
			GPNode sourceOfNode = getSourceGPNode(node);
			// SourceNode of replaced Node obtained
			GPNode sourceOfReplacement = getSourceGPNode(replacement);

			// update the ServiceEdge of sourceOfNode with that of
			// sourceOfReplacement

			Set<ServiceEdge> EdgeOfsourceOfReplacement = ((ServiceGPNode) sourceOfReplacement).getSemanticEdges();
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
					// redundant nodes in the tree transfered from the graph
					break;
				}
			}
		}
	}

	private GPNode getSourceGPNode(GPNode node) {

		GPNode parentNode = (GPNode) node.parent;

		GPNode sourceGPNode = null;

		// obtain SourceService if a ServiceGPNode is selected
		if (node instanceof ServiceGPNode) {

			GPNode pOperatorNode = (GPNode) parentNode.parent;
			GPNode[] ppOpratorNodeChild = pOperatorNode.children;
			for (GPNode ppOpChild : ppOpratorNodeChild) {
				if (ppOpChild instanceof ServiceGPNode) {
					sourceGPNode = ppOpChild;
				}
			}
		} else {
			// obtain SourceService if non ServiceGPNode is selected
			GPNode[] pOpratorNodeChild = parentNode.children;
			for (GPNode pOpChild : pOpratorNodeChild) {
				if (pOpChild instanceof ServiceGPNode) {
					sourceGPNode = pOpChild;
				}
			}
		}

		if (sourceGPNode == null) {
			System.out.println("Wrong SourceNode of selected Node obttained under crossover");
		}
		return sourceGPNode;
	}

	public void replaceNode4Mutation(GPNode node, GPNode replacement) {
		// Perform replacement if neither node is not null
		if (node != null && replacement != null) {
			// clone replacement
			// replacement = (GPNode) replacement.clone();

			GPNode[] replacementList = replacement.children.clone();
			for (GPNode gpNode : replacementList) {
				if (gpNode instanceof SequenceGPNode) {
					// update the replacement without including the startNode
					// and associated sequenceNode
					replacement = gpNode;
				}

				if (gpNode instanceof ServiceGPNode) {
					// obtain inComingEdge of StartNode from graph4Mutation
					Set<ServiceEdge> InComingEdgeOfNode = ((ServiceGPNode) gpNode).getSemanticEdges();
				}
			}

			
			
			
			// Reassign SemanticWeights if a single service Node selected.
			// Otherwise, semanticWeights are not needed to considered as a
			// weighted graph 4 mutation generated

			if (node instanceof ServiceGPNode) {

				// obtain targetService from selected service outgoingedge's
				// target
				String targetService = ((ServiceGPNode) node).getSemanticEdges().iterator().next().getTargetService();

				// obtain sourceService from replaceNode serviceName

				String sourceService = ((ServiceGPNode) replacement).getSerName();

				// get required inputSet

				if (!targetService.equals("endNode")) {
					WSCInitializer.serviceMap.get(targetService).getInputList();
				} else {

				}

				// get offerred outputSet

				WSCInitializer.serviceMap.get(sourceService).getOutputList();

				System.out.println("SingleService is selected 4 mutation" + node.toString() + " OurgoingEdge NO.:"
						+ ((ServiceGPNode) node).getSemanticEdges().size() + "TargetService:" + targetService);

				Set<ServiceEdge> updatedSemanticEdges = new HashSet<ServiceEdge>();

				ServiceEdge updatedServiceEdge = new ServiceEdge(0.11, 0.91);
				updatedServiceEdge.setTargetService(targetService);
				updatedServiceEdge.setSourceService(sourceService);
				updatedSemanticEdges.add(updatedServiceEdge);
				((ServiceGPNode) replacement).setSemanticEdges(updatedSemanticEdges);

			}

			GPNode parentNode = (GPNode) node.parent;
			// if (parentNode == null) {
			// the selected node is the topNode in the tree
			// super.trees[0].child = replacement;
			// } else {
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
