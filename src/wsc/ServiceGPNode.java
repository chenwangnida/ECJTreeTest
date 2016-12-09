package wsc;

import java.util.HashSet;
import java.util.Set;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class ServiceGPNode extends GPNode implements InOutNode {

	private static final long serialVersionUID = 1L;
	private Service service;

	private Set<String> inputs;
	private Set<String> outputs;

	public ServiceGPNode() {
		children = new GPNode[0];
	}

	public void eval(final EvolutionState state, final int thread, final GPData input, final ADFStack stack, final GPIndividual individual, final Problem problem) {
		WSCData rd = ((WSCData) (input));
		rd.maxTime = service.qos[WSCInitializer.TIME];
		rd.seenServices = new HashSet<Service>();
		rd.seenServices.add(service);
		rd.inputs = service.inputs;
		rd.outputs = service.outputs;

	    // Store input and output information in this node
        inputs = rd.inputs;
        outputs = rd.outputs;
	}

	public void setService(Service s) {
		service = s;
	}

//	@Override
//	public String toString() {
//		if (service == null)
//			return "null";
//		else
//			return service.name;
//	}

	@Override
	public String toString() {
		String serviceName;
		if (service == null)
			serviceName = "null";
		else
			serviceName = service.name;
		return String.format("%d [label=\"%s\"]; ", hashCode(), serviceName);
	}

	@Override
	public int expectedChildren() {
		return 0;
	}

	@Override
	public int hashCode() {
		if (service == null) {
			return "null".hashCode();
		}
		return super.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ServiceGPNode) {
			ServiceGPNode o = (ServiceGPNode) other;
			return service.name.equals(o.service.name);
		}
		else
			return false;
	}

//	@Override
//	public ServiceGPNode clone() {
//		ServiceGPNode newNode = new ServiceGPNode();
//		newNode.setService(service);
//	    newNode.inputs = inputs;
//	    newNode.outputs = outputs;
//		return newNode;
//	}

    public Set<String> getInputs() {
        return inputs;
    }

    public Set<String> getOutputs() {
        return outputs;
    }
}
