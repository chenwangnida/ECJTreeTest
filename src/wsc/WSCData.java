package wsc;

import java.util.Set;

import ec.gp.*;

public class WSCData extends GPData {

	private static final long serialVersionUID = 1L;
	public double maxTime;
	Set<Service> seenServices;
	public Set<String> inputs;
	public Set<String> outputs;

	public void copyTo(final GPData gpd) {
		WSCData wscd = (WSCData) gpd;
		wscd.maxTime = maxTime;
		wscd.seenServices = seenServices;
		wscd.inputs = inputs;
		wscd.outputs = outputs;
	}
}
