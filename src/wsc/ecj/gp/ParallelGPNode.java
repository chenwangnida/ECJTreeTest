package wsc.ecj.gp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

import wsc.data.pool.Service;
import wsc.graph.ServiceEdge;
import wsc.graph.ServiceInput;
import wsc.graph.ServiceOutput;
import wsc.graph.ServicePostcondition;
import wsc.graph.ServicePrecondition;

public class ParallelGPNode extends GPNode implements InOutNode {

	private static final long serialVersionUID = 1L;
	private List<ServiceInput> inputs;
	private List<ServiceOutput> outputs;
	private List<ServicePrecondition> preconditions;
	private List<ServicePostcondition> postconditions;
	private Set<ServiceEdge> semanticEdges;

	public List<ServiceInput> getInputs() {
		return inputs;
	}

	public void setInputs(List<ServiceInput> inputs) {
		this.inputs = inputs;
	}

	public List<ServiceOutput> getOutputs() {
		return outputs;
	}

	public void setOutputs(List<ServiceOutput> outputs) {
		this.outputs = outputs;
	}

	public List<ServicePrecondition> getPreconditions() {
		return preconditions;
	}

	public void setPreconditions(List<ServicePrecondition> preconditions) {
		this.preconditions = preconditions;
	}

	public List<ServicePostcondition> getPostconditions() {
		return postconditions;
	}

	public void setPostconditions(List<ServicePostcondition> postconditions) {
		this.postconditions = postconditions;
	}

	public Set<ServiceEdge> getSemanticEdges() {
		return semanticEdges;
	}

	public void setSemanticEdges(Set<ServiceEdge> semanticEdges) {
		this.semanticEdges = semanticEdges;
	}

	@Override
	public void eval(final EvolutionState state, final int thread, final GPData input, final ADFStack stack,
			final GPIndividual individual, final Problem problem) {
		double maxTime = 0.0;
		List<Service> seenServices = new ArrayList<Service>();
		List<ServiceInput> overallInputs = new ArrayList<ServiceInput>();
		List<ServiceOutput> overallOutputs = new ArrayList<ServiceOutput>();
		List<ServicePrecondition> overallPreconditions = new ArrayList<ServicePrecondition>();
		List<ServicePostcondition> overallPostconditions = new ArrayList<ServicePostcondition>();
		Set<ServiceEdge> overallServiceEdges = new HashSet<ServiceEdge>();

		WSCData rd = ((WSCData) (input));

		for (GPNode child : children) {
			child.eval(state, thread, input, stack, individual, problem);

			// Update max. time
			if (rd.maxTime > maxTime)
				maxTime = rd.maxTime;

			// Update seen services
			seenServices.addAll(rd.seenServices);

			// Update overall inputs and outputs
			overallInputs.addAll(rd.inputs);
			overallOutputs.addAll(rd.outputs);
			overallServiceEdges.addAll(rd.semanticEdges);

			// Update overall precondition and postcondition
			if (rd.preconditions != null && rd.postconditions != null) {
				overallPreconditions.addAll(rd.preconditions);
				overallPostconditions.addAll(rd.postconditions);
			}

		}

		// Finally, set the data with the overall values before exiting the
		// evaluation
		rd.maxTime = maxTime;
		rd.seenServices = seenServices;
		rd.inputs = overallInputs;
		rd.outputs = overallOutputs;
		rd.preconditions = overallPreconditions;
		rd.postconditions = overallPostconditions;
		rd.serviceId = "Parallel";
		rd.semanticEdges = overallServiceEdges;

		// Store input and output information in this node
		inputs = overallInputs;
		outputs = overallOutputs;
		preconditions = overallPreconditions;
		postconditions = overallPostconditions;
		semanticEdges = rd.semanticEdges;

//		for (ServiceInput i : overallInputs) {
//			state.output.println("Parallel:I:" + i.getInput(), 0);
//		}
//		for (ServiceOutput o : overallOutputs) {
//			state.output.println("Parallel:O:" +o.getOutput(), 0);
//		}

	}

	// @Override
	// public String toString() {
	// StringBuilder builder = new StringBuilder();
	// builder.append("Parallel(");
	// if (children != null) {
	// for (int i = 0; i < children.length; i++) {
	// GPNode child = children[i];
	// if (child != null)
	// builder.append(children[i].toString());
	// else
	// builder.append("null");
	// if (i != children.length - 1){
	// builder.append(",");
	// }
	// }
	// }
	// builder.append(")");
	// return builder.toString();
	// }
	//
	// @Override
	// public int expectedChildren() {
	// return 2;
	// }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("%d [label=\"Parallel\"]; ", hashCode()));
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				GPNode child = children[i];
				if (child != null) {
					builder.append(String.format("%d -> %d [dir=back]; ", hashCode(), children[i].hashCode()));
					builder.append(children[i].toString());
				}
			}
		}
		return builder.toString();
	}

	@Override
	public ParallelGPNode clone() {
		ParallelGPNode newNode = new ParallelGPNode();
		GPNode[] newChildren = new GPNode[children.length];
		for (int i = 0; i < children.length; i++) {
			newChildren[i] = (GPNode) children[i].clone();
			newChildren[i].parent = newNode;
		}
		newNode.children = newChildren;
		newNode.inputs = inputs;
		newNode.outputs = outputs;
		newNode.preconditions = preconditions;
		newNode.postconditions = postconditions;
		return newNode;
	}
}
