package wsc.ecj.gp;

import java.util.List;
import java.util.Set;

import ec.gp.*;
import wsc.data.pool.Service;
import wsc.graph.ServiceEdge;
import wsc.graph.ServiceInput;
import wsc.graph.ServiceOutput;
import wsc.graph.ServicePostcondition;
import wsc.graph.ServicePrecondition;

public class WSCData extends GPData {

	private static final long serialVersionUID = 1L;
	public double maxTime;
	List<Service> seenServices;
	
	public String serviceId;
	public List<ServiceInput> inputs;
	public List<ServiceOutput> outputs;
	public List<ServicePrecondition> preconditions;
	public List<ServicePostcondition> postconditions;
	public Set<ServiceEdge> semanticEdges; 
	
	public void copyTo(final GPData gpd) {
		WSCData wscd = (WSCData) gpd;
		wscd.serviceId = serviceId;
		wscd.maxTime = maxTime;
		wscd.seenServices = seenServices;
		wscd.inputs = inputs;
		wscd.outputs = outputs;
		wscd.preconditions = preconditions;
		wscd.postconditions = postconditions;
		wscd.semanticEdges = semanticEdges;
	}
}
