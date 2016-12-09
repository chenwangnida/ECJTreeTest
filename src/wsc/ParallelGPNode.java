package wsc;

import java.util.HashSet;
import java.util.Set;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class ParallelGPNode extends GPNode implements InOutNode {

	private static final long serialVersionUID = 1L;
	private Set<String> inputs;
	private Set<String> outputs;

	@Override
	public void eval(final EvolutionState state, final int thread, final GPData input, final ADFStack stack, final GPIndividual individual, final Problem problem) {
		double maxTime = 0.0;
		Set<Service> seenServices = new HashSet<Service>();
		Set<String> overallInputs = new HashSet<String>();
		Set<String> overallOutputs = new HashSet<String>();

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

		}

		// Finally, set the data with the overall values before exiting the evaluation
		rd.maxTime = maxTime;
		rd.seenServices = seenServices;
		rd.inputs = overallInputs;
		rd.outputs = overallOutputs;

		// Store input and output information in this node
		inputs = overallInputs;
		outputs = overallOutputs;
	}

//	@Override
//	public String toString() {
//		StringBuilder builder = new StringBuilder();
//		builder.append("Parallel(");
//		if (children != null) {
//    		for (int i = 0; i < children.length; i++) {
//    			GPNode child = children[i];
//    			if (child != null)
//    				builder.append(children[i].toString());
//    			else
//    				builder.append("null");
//    			if (i != children.length - 1){
//    				builder.append(",");
//    			}
//    		}
//		}
//		builder.append(")");
//		return builder.toString();
//	}
//
//	@Override
//	public int expectedChildren() {
//		return 2;
//	}

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
		return newNode;
	}

    public Set< String > getInputs() {
        return inputs;
    }

    public Set< String > getOutputs() {
        return outputs;
    }
}
