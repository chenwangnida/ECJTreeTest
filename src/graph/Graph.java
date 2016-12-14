package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedGraph;

import wsc.graph.ServiceEdge;

public class Graph<String, ServiceEdge> extends DefaultDirectedGraph<String, ServiceEdge>{

	private static final long serialVersionUID = -1161512349678026436L;
	public Graph(Class edgeClass) {
		super(edgeClass);
		// TODO Auto-generated constructor stub
	}
	/**
	 *
	 */
	public Map<String, GraphNode> nodeMap = new HashMap<String, GraphNode>();
	public List<ServiceEdge> edgeList = new ArrayList<ServiceEdge>();

	@Override
	public java.lang.String toString() {
		StringBuilder builder = new StringBuilder();


		builder.append("digraph g {");
		for (ServiceEdge e :edgeList) {
			builder.append(e.toString());
			builder.append("; ");
		}
		builder.append("}");
		return builder.toString();
	}
}
