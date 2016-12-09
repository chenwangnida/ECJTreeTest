package graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ec.gp.GPNode;
import wsc.ParallelGPNode;
import wsc.SequenceGPNode;
import wsc.Service;
import wsc.ServiceGPNode;
import wsc.TaxonomyNode;

public class GraphNode implements Cloneable {
	private List<GraphEdge> incomingEdgeList = new ArrayList<GraphEdge>();
	private List<GraphEdge> outgoingEdgeList = new ArrayList<GraphEdge>();
	private List<TaxonomyNode> taxonomyOutputs = new ArrayList<TaxonomyNode>();
	private Service serv;

	public GraphNode(Service serv) {
		this.serv = serv;
	}

	public List<GraphEdge> getIncomingEdgeList() {
		return incomingEdgeList;
	}

	public List<GraphEdge> getOutgoingEdgeList() {
		return outgoingEdgeList;
	}

	public double[] getQos() {
		return serv.qos;
	}

	public Set<String> getInputs() {
		return serv.inputs;
	}

	public Set<String> getOutputs() {
		return serv.outputs;
	}

	public String getName() {
		return serv.name;
	}

	public GraphNode clone() {
		return new GraphNode(serv);
	}

	public Service getService() {
		return serv;
	}

	public List<TaxonomyNode> getTaxonomyOutputs() {
		return taxonomyOutputs;
	}

	@Override
	public String toString(){
		return serv.name;
	}

	@Override
	public int hashCode() {
		return serv.name.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GraphNode) {
			GraphNode o = (GraphNode) other;
			return serv.name.equals(o.serv.name);
		}
		else
			return false;
	}

	/**
	 * Indirectly recursive method that transforms this GraphNode and all nodes
	 * that directly or indirectly receive its output into a tree
	 * representation.
	 *
	 * @return Tree root
	 */
	public GPNode toTree() {
		GPNode root = null;
		if (serv.getName().equals("start")) {
			// Start with sequence
			if (outgoingEdgeList.size() == 1) {
				/*
				 * If the next node points to the output, this is a
				 * single-service composition, so return a service node
				 */
				GraphEdge next = outgoingEdgeList.get(0);
				root = getNode(next.getToNode());
			}
			// Start with parallel node
			else if (outgoingEdgeList.size() > 1)
				root = createParallelNode(this, outgoingEdgeList);
		} else {
			// Begin by checking how many nodes are in the right child.
			GPNode rightChild;

			List<GraphEdge> children = new ArrayList<GraphEdge>(
					outgoingEdgeList);

			// Find the end node in the list, if it is contained there
			GraphEdge outputEdge = null;
			for (GraphEdge ch : children) {
				if (ch.getToNode().getName().equals("end")) {
					outputEdge = ch;
					break;
				}
			}
			// Remove the output node from the children list
			children.remove(outputEdge);

			// If there is only one other child, create a sequence construct
			if (children.size() == 1) {
				rightChild = getNode(children.get(0).getToNode());
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setService(serv);
				root = createSequenceNode(sgp, rightChild);
			}
			// Else if there are no children at all, return a new leaf node
			else if (children.size() == 0) {
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setService(serv);
				root = sgp;
			}
			// Else, create a new parallel construct wrapped in a sequence
			// construct
			else {
				rightChild = createParallelNode(this, children);
				ServiceGPNode sgp = new ServiceGPNode();
				sgp.setService(serv);
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
	private GPNode createParallelNode(GraphNode n, List<GraphEdge> childrenGraphNodes) {
		GPNode root = new ParallelGPNode();

		// Create subtrees for children
		int length = childrenGraphNodes.size();
		GPNode[] children = new GPNode[length];

		for (int i = 0; i < length; i++) {
			GraphEdge child = childrenGraphNodes.get(i);
			children[i] = getNode(child.getToNode());
			children[i].parent = root;
		}
		root.children = children;
		return root;
	}

	/**
	 * Represents a GraphNode with a single outgoing edge as a SequenceNode in
	 * the tree (edges to the Output node are not counted). The left and right
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

	/**
	 * Retrieves the tree representation for the provided GraphNode,
	 * also checking if should translate to a leaf.
	 *
	 * @param n
	 * @return root of tree translation
	 */
	private GPNode getNode(GraphNode n) {
		GPNode result;
		if (isLeaf(n)) {
			ServiceGPNode sgp = new ServiceGPNode();
			sgp.setService(n.serv);
			result = sgp;
		}
		// Otherwise, make next node's subtree the right child
		else
			result = n.toTree();
		return result;
	}

	/**
	 * Verify whether the GraphNode provided translates into a
	 * leaf node when converting the graph into a tree.
	 *
	 * @param node
	 * @return True if it translates into a leaf node,
	 * false otherwise
	 */
	private boolean isLeaf(GraphNode node) {
		return node.outgoingEdgeList.size() == 1 && node.outgoingEdgeList.get(0).getToNode().getName().equals("Output");
	}
}
