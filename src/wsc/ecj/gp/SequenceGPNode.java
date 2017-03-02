package wsc.ecj.gp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
import wsc.owl.bean.OWLClass;

public class SequenceGPNode extends GPNode implements InOutNode {

	private static final long serialVersionUID = 1L;
	private List<ServiceInput> inputs;
	private List<ServiceOutput> outputs;
	private List<ServicePrecondition> preconditions;
	private List<ServicePostcondition> postconditions;
	private List<ServiceEdge> semanticEdges;

	@Override
	public List<ServiceInput> getInputs() {
		return inputs;
	}

	public void setInputs(List<ServiceInput> inputs) {
		this.inputs = inputs;
	}

	@Override
	public List<ServiceOutput> getOutputs() {
		return outputs;
	}

	public void setOutputs(List<ServiceOutput> outputs) {
		this.outputs = outputs;
	}

	@Override
	public List<ServicePrecondition> getPreconditions() {
		return preconditions;
	}

	public void setPreconditions(List<ServicePrecondition> preconditions) {
		this.preconditions = preconditions;
	}

	@Override
	public List<ServicePostcondition> getPostconditions() {
		return postconditions;
	}

	public void setPostconditions(List<ServicePostcondition> postconditions) {
		this.postconditions = postconditions;
	}

	public List<ServiceEdge> getSemanticEdges() {
		return semanticEdges;
	}

	public void setSemanticEdges(List<ServiceEdge> semanticEdges) {
		this.semanticEdges = semanticEdges;
	}

	@Override
	public void eval(final EvolutionState state, final int thread, final GPData input, final ADFStack stack,
			final GPIndividual individual, final Problem problem) {
		double maxTime = 0.0;
		Set<Service> seenServices = new HashSet<Service>();
		List<ServiceInput> overallInputs = new ArrayList<ServiceInput>();
		List<ServiceOutput> overallOutputs = new ArrayList<ServiceOutput>();
		List<ServiceInput> lChildInputs = new ArrayList<ServiceInput>();
		List<ServiceOutput> lChildOutputs = new ArrayList<ServiceOutput>();
		List<ServiceInput> rChildInputs = new ArrayList<ServiceInput>();
		List<ServiceOutput> rChildOutputs = new ArrayList<ServiceOutput>();

		WSCInitializer init = (WSCInitializer) state.initializer;
		WSCData rd = ((WSCData) (input));

		// leftChild
		children[0].eval(state, thread, input, stack, individual, problem);

		if (!rd.serviceId.equals("startNode") && !rd.serviceId.equals("endNode")) {

			// Update max. time
			maxTime += rd.maxTime;

			// Update seen services
			seenServices.addAll(rd.seenServices);

			// Load all Inputs, Outputs, Preconditions and Postconditions of
			// Children
			lChildInputs.addAll(rd.inputs);
			lChildOutputs.addAll(rd.outputs);

		}
		children[1].eval(state, thread, input, stack, individual, problem);

		if (!rd.serviceId.equals("startNode") && !rd.serviceId.equals("endNode")) {

			// Update max. time
			maxTime += rd.maxTime;

			// Update seen services
			seenServices.addAll(rd.seenServices);

			// Load all Inputs, Outputs, Preconditions and Postconditions of
			// Children
			rChildInputs.addAll(rd.inputs);
			rChildOutputs.addAll(rd.outputs);

		}

		overallInputs.addAll(lChildInputs);

		List<ServiceInput> removedInputs = new ArrayList<ServiceInput>();

		// remove inputs produced by proccesor web services
		for (ServiceOutput serOutput : lChildOutputs) {
			isContainedOfromI(serOutput, rChildInputs, init, removedInputs);
		}
		if (removedInputs != null) {
			for (ServiceInput serInput4remove : removedInputs) {
				Iterator<ServiceInput> iterator = rChildInputs.iterator();
				while (iterator.hasNext()) {
					ServiceInput serInput = iterator.next();
					if ((serInput.getInput()).equals(serInput4remove.getInput())) {
						iterator.remove();
					}
				}
			}
		}

		if (rChildInputs != null) {
			overallInputs.addAll(rChildInputs);
		}

		overallOutputs.addAll(rChildOutputs);

//		List<ServiceOutput> removedOutputs = new ArrayList<ServiceOutput>();
//
//		for (ServiceInput serInput : rChildInputs) {
//			isContainedIfromO(serInput, lChildOutputs, init, removedOutputs);
//		}
//		if (removedOutputs != null) {
//			for (ServiceOutput serOutput4remove : removedOutputs) {
//				Iterator<ServiceOutput> iterator = lChildOutputs.iterator();
//				while (iterator.hasNext()) {
//					ServiceOutput serInput = iterator.next();
//					if ((serInput.getOutput()).equals(serOutput4remove.getOutput())) {
//						iterator.remove();
//					}
//				}
//			}
//		}

		if (lChildOutputs != null) {
			overallOutputs.addAll(lChildOutputs);
		}

		List<Service> seenServices1 = new ArrayList<Service>(seenServices);
//		List<ServiceInput> overallInputs1 = new ArrayList<ServiceInput>(overallInputs);
//		List<ServiceOutput> overallOutputs1 = new ArrayList<ServiceOutput>(overallOutputs);
		
		
		
		// Finally, set the data with the overall values before exiting the
		// evaluation
		rd.maxTime = maxTime;
		rd.seenServices = seenServices1;
		rd.inputs = overallInputs;
		rd.outputs = overallOutputs;
		rd.serviceId = "Sequence";

		// Store input and output information in this node
		inputs = rd.inputs;
		outputs = rd.outputs;
		preconditions = rd.preconditions;
		postconditions = rd.postconditions;
		semanticEdges = rd.semanticEdges;
	}

	// check there is inputs produced by the services Outputs or not
	private List isContainedOfromI(ServiceOutput serOutput, List<ServiceInput> overallInputs, WSCInitializer init,
			List<ServiceInput> overallInputsRemoved) {
		for (ServiceInput serInputs : overallInputs) {

			OWLClass givenClass = WSCInitializer.initialWSCPool.getSemanticsPool().getOwlClassHashMap()
					.get(WSCInitializer.initialWSCPool.getSemanticsPool().getOwlInstHashMap().get(serOutput.getOutput())
							.getRdfType().getResource().substring(1));
			OWLClass relatedClass = WSCInitializer.initialWSCPool.getSemanticsPool().getOwlClassHashMap()
					.get(WSCInitializer.initialWSCPool.getSemanticsPool().getOwlInstHashMap().get(serInputs.getInput())
							.getRdfType().getResource().substring(1));

			String a = givenClass.getID();
			String b = relatedClass.getID();
			// System.out.println(giveninput+" concept of "+a+";"+existInput+"
			// concept of" +b);
//			if(a.equals("book")&&b.equals("novel")){
//				System.out.println("enter debug");
//			}

			if (WSCInitializer.semanticMatrix.get(a, b) != null) {
				overallInputsRemoved.add(serInputs);
				return overallInputsRemoved;
			}
		}
		return overallInputsRemoved;
	}

	// check there is inputs produced by the services Outputs or not
	private List isContainedOfromIMatrix(ServiceOutput serOutput, List<ServiceInput> overallInputs, WSCInitializer init,
			List<ServiceInput> overallInputsRemoved) {
		for (ServiceInput serInputs : overallInputs) {

			OWLClass givenClass = WSCInitializer.initialWSCPool.getSemanticsPool().getOwlClassHashMap()
					.get(WSCInitializer.initialWSCPool.getSemanticsPool().getOwlInstHashMap().get(serOutput.getOutput())
							.getRdfType().getResource().substring(1));
			OWLClass relatedClass = WSCInitializer.initialWSCPool.getSemanticsPool().getOwlClassHashMap()
					.get(WSCInitializer.initialWSCPool.getSemanticsPool().getOwlInstHashMap().get(serInputs.getInput())
							.getRdfType().getResource().substring(1));

			String a = givenClass.getID();
			String b = relatedClass.getID();

			if (WSCInitializer.semanticMatrix.get(a, b) != null) {
				overallInputsRemoved.add(serInputs);
				return overallInputsRemoved;
			}
		}
		return null;
	}

	// check there is inputs produced by the services Outputs or not
	private List<ServiceOutput> isContainedIfromO(ServiceInput serInput, List<ServiceOutput> overallOutput,
			WSCInitializer init, List<ServiceOutput> overallOutputsRemoved) {
		for (ServiceOutput serOutput : overallOutput) {

			OWLClass givenClass = WSCInitializer.initialWSCPool.getSemanticsPool().getOwlClassHashMap()
					.get(WSCInitializer.initialWSCPool.getSemanticsPool().getOwlInstHashMap().get(serInput.getInput())
							.getRdfType().getResource().substring(1));
			OWLClass relatedClass = WSCInitializer.initialWSCPool.getSemanticsPool().getOwlClassHashMap()
					.get(WSCInitializer.initialWSCPool.getSemanticsPool().getOwlInstHashMap().get(serOutput.getOutput())
							.getRdfType().getResource().substring(1));

			String a = givenClass.getID();
			String b = relatedClass.getID();
			// System.out.println(giveninput+" concept of "+a+";"+existInput+"
			// concept of" +b);

			if (WSCInitializer.semanticMatrix.get(a, b) != null) {
				overallOutputsRemoved.add(serOutput);
				return overallOutputsRemoved;
			}
		}

		return null;
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
