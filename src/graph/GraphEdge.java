package graph;

import java.util.Set;

public class GraphEdge {
	private GraphNode fromNode;
	private GraphNode toNode;
	private Set<String> intersect;

	public GraphEdge(Set<String> intersect) {
		this.intersect = intersect;
	}

	public GraphNode getFromNode() {
		return fromNode;
	}

	public GraphNode getToNode() {
		return toNode;
	}

	public void setFromNode(GraphNode fromNode) {
		this.fromNode = fromNode;
	}

	public void setToNode(GraphNode toNode) {
		this.toNode = toNode;
	}

	public Set<String> getIntersect() {
		return intersect;
	}

	@Override
	public String toString() {
		return String.format("%s->%s", fromNode, toNode);
	}

	@Override
	public int hashCode() {
		return (fromNode.getName() + toNode.getName()).hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof GraphEdge) {
			GraphEdge o = (GraphEdge) other;
			return fromNode.getName().equals(o.fromNode.getName()) && toNode.getName().equals(o.toNode.getName());
		}
		else
			return false;
	}
}
