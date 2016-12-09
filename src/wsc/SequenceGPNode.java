package wsc;

import java.util.HashSet;
import java.util.Set;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class SequenceGPNode extends GPNode implements InOutNode {

	private static final long serialVersionUID = 1L;
	private Set<String> inputs;
	private Set<String> outputs;

	@Override
	public void eval(final EvolutionState state, final int thread, final GPData input, final ADFStack stack, final GPIndividual individual, final Problem problem) {
		double maxTime = 0.0;
		Set<Service> seenServices = new HashSet<Service>();

		WSCData rd = ((WSCData) (input));

		children[0].eval(state, thread, input, stack, individual, problem);
		maxTime = rd.maxTime;
		seenServices = rd.seenServices;
		Set<String> in = rd.inputs;

		children[1].eval(state, thread, input, stack, individual, problem);
		rd.maxTime += maxTime;
		rd.seenServices.addAll(seenServices);
		// The inputs should be those of the left child, but the outputs and max layer are just those of the right child (already retrieved)
		rd.inputs = in;

	    // Store input and output information in this node
        inputs = rd.inputs;
        outputs = rd.outputs;
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

	@Override
	public int expectedChildren() {
		return 2;
	}

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
		return newNode;
	}

    public Set< String > getInputs() {
        return inputs;
    }

    public Set< String > getOutputs() {
        return outputs;
    }
}
