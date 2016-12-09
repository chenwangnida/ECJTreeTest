package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
	public Map<String, GraphNode> nodeMap = new HashMap<String, GraphNode>();
	public List<GraphEdge> edgeList = new ArrayList<GraphEdge>();

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("digraph g {");
		for (GraphEdge e : edgeList) {
			builder.append(e.toString());
			builder.append("; ");
		}
		builder.append("}");
		return builder.toString();
	}
}
