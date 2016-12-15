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
import wsc.graph.ServiceInput;
import wsc.graph.ServiceOutput;
import wsc.graph.ServicePostcondition;
import wsc.graph.ServicePrecondition;

public class SequenceGPNode extends GPNode {

	private static final long serialVersionUID = 1L;
	private List<ServiceInput> inputs;
	private List<ServiceOutput> outputs;
	private List<ServicePrecondition> preconditions;
	private List<ServicePostcondition> postconditions;

	@Override
	public void eval(final EvolutionState state, final int thread, final GPData input, final ADFStack stack,
			final GPIndividual individual, final Problem problem) {
		double maxTime = 0.0;
		List<Service> seenServices = new ArrayList<Service>();
		List<ServiceInput> overallInputs = new ArrayList<ServiceInput>();
		List<ServiceOutput> overallOutputs = new ArrayList<ServiceOutput>();
		List<ServicePrecondition> overallPreconditions = new ArrayList<ServicePrecondition>();
		List<ServicePostcondition> overallPostconditions = new ArrayList<ServicePostcondition>();


		WSCData rd = ((WSCData) (input));
		for (GPNode child : children) {
			child.eval(state, thread, input, stack, individual, problem);
			
			// Update max. time
			maxTime += rd.maxTime;
			
			// Update seen services
			seenServices.addAll(rd.seenServices);
			
			// Load all Inputs, Outputs, Preconditions and Postconditions of Children
			overallInputs.addAll(rd.inputs);
			overallOutputs.addAll(rd.outputs);						
			overallPreconditions.addAll(rd.preconditions);
			overallPostconditions.addAll(rd.postconditions);
		
		}
				
		overallInputs.removeAll(overallOutputs);
		overallOutputs.removeAll(overallInputs);
		overallPreconditions.removeAll(overallPostconditions);
		overallPostconditions.removeAll(overallPreconditions);		

//		children[0].eval(state, thread, input, stack, individual, problem);
//		maxTime = rd.maxTime;
//		seenServices = rd.seenServices;
//		Set<String> in = rd.inputs;
//
//		children[1].eval(state, thread, input, stack, individual, problem);
//		rd.maxTime += maxTime;
//		rd.seenServices.addAll(seenServices);
//		rd.inputs = in;
		
		
		
		// Finally, set the data with the overall values before exiting the
		// evaluation
		rd.maxTime = maxTime;
		rd.seenServices = seenServices;
		rd.inputs = overallInputs;
		rd.outputs = overallOutputs;
		rd.preconditions = overallPreconditions;
		rd.postconditions = overallPostconditions;

		// Store input and output information in this node
		inputs = rd.inputs;
		outputs = rd.outputs;
		preconditions = rd.preconditions;
		postconditions = rd.postconditions;
		
		
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("%d [label=\"Sequence\"]; ", hashCode()));
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

	// @Override
	// public int expectedChildren() {
	// return 2;
	// }

	@Override
	public SequenceGPNode clone() {
		SequenceGPNode newNode = new SequenceGPNode();
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
