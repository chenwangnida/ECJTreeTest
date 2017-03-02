package wsc.ecj.gp;

import ec.util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ec.*;
import ec.gp.*;
import ec.simple.*;
import wsc.data.pool.Service;
import wsc.graph.ParamterConn;
import wsc.graph.ServiceEdge;
import wsc.graph.ServiceInput;
import wsc.graph.ServiceOutput;

public class WSC extends GPProblem implements SimpleProblemForm {

	private static final long serialVersionUID = 1L;

	@Override
	public void setup(final EvolutionState state, final Parameter base) {
		// very important, remember this
		super.setup(state, base);

		// verify our input is the right class (or subclasses from it)
		if (!(input instanceof WSCData))
			state.output.fatal("GPData class must subclass from " + WSCData.class, base.push(P_DATA), null);
	}

	@Override
	public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
			final int threadnum) {
		if (!ind.evaluated) {
			WSCInitializer init = (WSCInitializer) state.initializer;
			WSCData input = (WSCData) (this.input);
			

			GPIndividual gpInd = (GPIndividual) ind;

			// state.output.println("Evaluate new Individual:" +
			// gpInd.toString(), 0);

			gpInd.trees[0].child.eval(state, threadnum, input, stack, ((GPIndividual) ind), this);
			
			// evaluate semantic matchmaking quality
			Set<ServiceEdge> semanticEdges = calculateSemanticQuality(gpInd);

			// evaluate QoS
			
			double[] qos = new double[4];
			qos[WSCInitializer.TIME] = input.maxTime;
			qos[WSCInitializer.AVAILABILITY] = 1.0;
			qos[WSCInitializer.RELIABILITY] = 1.0;

			double mt = 1.0;
			double dst = 0.0; // Exact Match dst = 1 ;
			Set<SemanticLink> semanticLinks = new HashSet<SemanticLink>();

			for (ServiceEdge edge : semanticEdges) {
				SemanticLink sl = new SemanticLink();

				sl.setSourceService(edge.getSourceService());
				sl.setTargetService(edge.getTargetService());
				sl.setAvgmt(edge.getAvgmt());
				sl.setAvgsdt(edge.getAvgsdt());
				semanticLinks.add(sl);
			}

			for (SemanticLink semanticQuality : semanticLinks) {
				mt *= semanticQuality.getAvgmt();
				dst += semanticQuality.getAvgsdt();

			}

			dst = dst / (semanticLinks.size());

			for (Service s : input.seenServices) {
				qos[WSCInitializer.COST] += s.qos[WSCInitializer.COST];
				qos[WSCInitializer.AVAILABILITY] *= s.qos[WSCInitializer.AVAILABILITY];
				qos[WSCInitializer.RELIABILITY] *= s.qos[WSCInitializer.RELIABILITY];
			}

			double fitness = calculateFitness(qos[WSCInitializer.AVAILABILITY], qos[WSCInitializer.RELIABILITY],
					qos[WSCInitializer.TIME], qos[WSCInitializer.COST], mt, dst, init);

			// state.output.println("fitnessValue:"+fitness, 0);
			// the fitness better be SimpleFitness!
			SimpleFitness f = ((SimpleFitness) ind.fitness);
			//
			String fitnessStr = fitness + "";
			String f0 = "0.8329982119033941";
			if (fitnessStr.startsWith(f0)) {
				double qosvalue = calculateQoS(qos[WSCInitializer.AVAILABILITY], qos[WSCInitializer.RELIABILITY],
						qos[WSCInitializer.TIME], qos[WSCInitializer.COST], init);
				double smvalue = calculateSM(mt, dst, init);
				state.output.println(fitness + ";" + "QoS" + qosvalue + ";SM" + smvalue, 0);

//				for (Service s : input.seenServices) {
//					qos[WSCInitializer.COST] += s.qos[WSCInitializer.COST];
//					qos[WSCInitializer.AVAILABILITY] *= s.qos[WSCInitializer.AVAILABILITY];
//					qos[WSCInitializer.RELIABILITY] *= s.qos[WSCInitializer.RELIABILITY];
//				}

				input.seenServices.forEach(ser -> System.out.print(ser.getServiceID() + ";"));
				for (ServiceEdge semanticQuality : semanticEdges) {
					System.out.println(semanticQuality.getSourceService() + "->" + semanticQuality.getTargetService()
							+ ";avgmt:" + semanticQuality.getAvgmt() + ";avgdst:" + semanticQuality.getAvgsdt());

				}
				state.output.println("Where is the fucking wrong Individual:" + gpInd.toString(), 0);

			}

			f.setFitness(state, fitness, false);
			// f.setStandardizedFitness(state, fitness);
			ind.evaluated = true;
		}
	}

	// private double calculateFitness(double a, double r, double t, double c,
	// WSCInitializer init) {
	// a = normaliseAvailability(a, init);
	// r = normaliseReliability(r, init);
	// t = normaliseTime(t, init);
	// c = normaliseCost(c, init);
	//
	// double fitness = ((init.w1 * a) + (init.w2 * r) + (init.w3 * t) +
	// (init.w4 * c));
	// return fitness;
	// }

	private Set<ServiceEdge> calculateSemanticQuality(GPIndividual gpInd) {
		// get all serviceNodes not including endNodes
		List<GPNode> serNodes = ((WSCIndividual) gpInd).getAllServiceGPNodes();
		Set<ServiceEdge> serviceEdgeSet = new HashSet<ServiceEdge>();
		int situation = 0;

		for (GPNode serNode : serNodes) {
			InOutNode serIO = (InOutNode) serNode;
			// obtain the neighbor node
			GPNode parentNode = (GPNode) (serNode.parent);
			GPNode neighborNode;
			if (parentNode.children[0] == serNode) {
				neighborNode = parentNode.children[1];
			} else {
				neighborNode = parentNode.children[0];
			}

			InOutNode neighborNodeIO = (InOutNode) (neighborNode);

			String sourceSerId = ((ServiceGPNode) serIO).getSerName();

			if (sourceSerId == "startNode") {
				situation = 1;
			} else if ((parentNode.children[0] instanceof ServiceGPNode)
					&& (parentNode.children[1] instanceof ServiceGPNode)) {
				situation = 2;

			} else {
				situation = 3;
			}

			switch (situation) {
			case 1:
				List<ServiceOutput> serOutput1 = new ArrayList<ServiceOutput>();
				WSCInitializer.taskInput
						.forEach(taskInputStr -> serOutput1.add(new ServiceOutput(taskInputStr, false)));
				serOutput1.forEach(serO -> serO.setServiceId("startNode"));
				List<ServiceInput> neighborNodeInput1 = neighborNodeIO.getInputs();
				serviceEdgeSet.addAll(aggregateSemanticLink(neighborNodeInput1, serOutput1, sourceSerId));
				break;
			case 2:
				List<ServiceOutput> serOutput2 = serIO.getOutputs();
				List<ServiceInput> neighborNodeInput2 = new ArrayList<ServiceInput>();
				WSCInitializer.taskOutput
						.forEach(taskOutputStr -> neighborNodeInput2.add(new ServiceInput(taskOutputStr, false)));
				neighborNodeInput2.forEach(endNode -> endNode.setServiceId("endNode"));
				serviceEdgeSet.addAll(aggregateSemanticLink(neighborNodeInput2, serOutput2, sourceSerId));
				break;
			case 3:
				List<ServiceOutput> serOutput3 = serIO.getOutputs();
				List<ServiceInput> neighborNodeInput3 = neighborNodeIO.getInputs();
				serviceEdgeSet.addAll(aggregateSemanticLink(neighborNodeInput3, serOutput3, sourceSerId));
				break;

			}

		}
		return serviceEdgeSet;
	}

	private double calculateFitness(double a, double r, double t, double c, double mt, double dst,
			WSCInitializer init) {

		a = normaliseAvailability(a);
		r = normaliseReliability(r);
		t = normaliseTime(t);
		c = normaliseCost(c);
		mt = normaliseMatchType(mt);
		dst = normaliseDistanceValue(dst);

		double fitness = init.w1 * a + init.w2 * r + init.w3 * t + init.w4 * c + init.w5 * mt + init.w6 * dst;

		return fitness;
	}

	private double calculateQoS(double a, double r, double t, double c, WSCInitializer init) {
		System.out.println("Before :a:" + a + "r:" + r + "t:" + t + "c:" + c);

		a = normaliseAvailability(a);
		r = normaliseReliability(r);
		t = normaliseTime(t);
		c = normaliseCost(c);
		System.out.println("a:" + a + "r:" + r + "t:" + t + "c:" + c);
		double fitness = init.w1 * a + init.w2 * r + init.w3 * t + init.w4 * c;

		return fitness;
	}

	private double calculateSM(double mt, double dst, WSCInitializer init) {
		System.out.println("mt before:" + mt + ";dst before:" + dst);

		mt = normaliseMatchType(mt);
		dst = normaliseDistanceValue(dst);

		double fitness = init.w5 * mt + init.w6 * dst;
		System.out.println("mt:" + mt + ";dst:" + dst);

		return fitness;
	}

	private double normaliseMatchType(double matchType) {
		if (WSCInitializer.MAXINUM_MATCHTYPE - WSCInitializer.MINIMUM_MATCHTYPE == 0.0)
			return 1.0;
		else
			return (matchType - WSCInitializer.MINIMUM_MATCHTYPE)
					/ (WSCInitializer.MAXINUM_MATCHTYPE - WSCInitializer.MINIMUM_MATCHTYPE);
	}

	private double normaliseDistanceValue(double distanceValue) {
		if (WSCInitializer.MAXINUM_SEMANTICDISTANCE - WSCInitializer.MININUM_SEMANTICDISTANCE == 0.0)
			return 1.0;
		else
			return (distanceValue - WSCInitializer.MININUM_SEMANTICDISTANCE)
					/ (WSCInitializer.MAXINUM_SEMANTICDISTANCE - WSCInitializer.MININUM_SEMANTICDISTANCE);
	}

	public double normaliseAvailability(double availability) {
		if (WSCInitializer.MAXIMUM_AVAILABILITY - WSCInitializer.MINIMUM_AVAILABILITY == 0.0)
			return 1.0;
		else
			return (availability - WSCInitializer.MINIMUM_AVAILABILITY)
					/ (WSCInitializer.MAXIMUM_AVAILABILITY - WSCInitializer.MINIMUM_AVAILABILITY);
	}

	public double normaliseReliability(double reliability) {
		if (WSCInitializer.MAXIMUM_RELIABILITY - WSCInitializer.MINIMUM_RELIABILITY == 0.0)
			return 1.0;
		else
			return (reliability - WSCInitializer.MINIMUM_RELIABILITY)
					/ (WSCInitializer.MAXIMUM_RELIABILITY - WSCInitializer.MINIMUM_RELIABILITY);
	}

	public double normaliseTime(double time) {
		if (WSCInitializer.MAXIMUM_TIME - WSCInitializer.MINIMUM_TIME == 0.0)
			return 1.0;
		else
			return (WSCInitializer.MAXIMUM_TIME - time) / (WSCInitializer.MAXIMUM_TIME - WSCInitializer.MINIMUM_TIME);
	}

	public double normaliseCost(double cost) {
		if (WSCInitializer.MAXIMUM_COST - WSCInitializer.MINIMUM_COST == 0.0)
			return 1.0;
		else
			return (WSCInitializer.MAXIMUM_COST - cost) / (WSCInitializer.MAXIMUM_COST - WSCInitializer.MINIMUM_COST);
	}

	// private double normaliseAvailability(double availability, WSCInitializer
	// init) {
	// if (init.maxAvailability - init.minAvailability == 0.0)
	// return 1.0;
	// else
	// return (availability - init.minAvailability)/(init.maxAvailability -
	// init.minAvailability);
	// }
	//
	// private double normaliseReliability(double reliability, WSCInitializer
	// init) {
	// if (init.maxReliability - init.minReliability == 0.0)
	// return 1.0;
	// else
	// return (reliability - init.minReliability)/(init.maxReliability -
	// init.minReliability);
	// }
	//
	// private double normaliseTime(double time, WSCInitializer init) {
	// // If the time happens to go beyond the normalisation bound, set it to
	// the normalisation bound
	// if (time > init.maxTime)
	// time = init.maxTime;
	//
	// if (init.maxTime - init.minTime == 0.0)
	// return 1.0;
	// else
	// return (init.maxTime - time)/(init.maxTime - init.minTime);
	// }
	//
	// private double normaliseCost(double cost, WSCInitializer init) {
	// // If the cost happens to go beyond the normalisation bound, set it to
	// the normalisation bound
	// if (cost > init.maxCost)
	// cost = init.maxCost;
	//
	// if (init.maxCost - init.minCost == 0.0)
	// return 1.0;
	// else
	// return (init.maxCost - cost)/(init.maxCost - init.minCost);
	// }
	/**
	 * check whether output is required by the defined required Output
	 *
	 * @param givenoutput
	 * @return
	 */
	private Set<ServiceEdge> aggregateSemanticLink(List<ServiceInput> neighborNodeInput, List<ServiceOutput> serOutput,
			String sourceSerId) {
		List<ParamterConn> pConnList = new ArrayList<ParamterConn>();
		Set<String> targetSerIdSet = new HashSet<String>();
		Set<ServiceEdge> serEdgeList = new HashSet<ServiceEdge>();

		double summt;
		double sumdst;

		neighborNodeInput.forEach(parentI -> parentI.setSatified(false));
		serOutput.forEach(serO -> serO.setSatified(false));

		for (int j = 0; j < serOutput.size(); j++) {

			String outputInst = serOutput.get(j).getOutput();

			for (int i = 0; i < neighborNodeInput.size(); i++) {
				ServiceInput parentInputReuqired = neighborNodeInput.get(i);

				String inputrequired = parentInputReuqired.getInput();
				String targetSerId = parentInputReuqired.getServiceId();

				if (!parentInputReuqired.isSatified()) {

					ParamterConn pConn = WSCInitializer.getInitialWSCPool().getSemanticsPool()
							.searchSemanticMatchTypeFromInst(outputInst, inputrequired);
					pConn.setOutputInst(outputInst);
					pConn.setOutputrequ(inputrequired);

					boolean foundmatched = pConn.isConsidered();
					if (foundmatched) {
						parentInputReuqired.setSatified(true);

						// if (graphOutputListMap.get(outputInst) == null) {
						// pConn.setSourceServiceID("startNode");
						// System.err.println(outputInst+"Inst not in the
						// map");
						// } else {
						pConn.setSourceServiceID(sourceSerId);
						pConn.setTargetServiceID(targetSerId);
						pConnList.add(pConn);
						// break ;
					}
				}
			}
		}

		for (ParamterConn p : pConnList) {
			String targetSerId = p.getTargetServiceID();
			targetSerIdSet.add(targetSerId);
		}

		for (String targetSerId : targetSerIdSet) {
			ServiceEdge serEdge = new ServiceEdge(0, 0);
			serEdge.setSourceService(sourceSerId);
			serEdge.setTargetService(targetSerId);
			for (ParamterConn p : pConnList) {
				if (p.getTargetServiceID().equals(targetSerId)) {
					serEdge.getpConnList().add(p);
				}
			}
			serEdgeList.add(serEdge);
		}

		for (ServiceEdge edge : serEdgeList) {
			summt = 0.00;
			sumdst = 0.00;
			for (int i1 = 0; i1 < edge.getpConnList().size(); i1++) {
				ParamterConn pCo = edge.getpConnList().get(i1);
				// pCo.setTargetServiceID("endNode");
				// set OriginalTargetServiceId from the node selected for
				// mutation.
				summt += pCo.getMatchType();
				sumdst += pCo.getSimilarity();

			}
			int count = edge.getpConnList().size();
			edge.setAvgmt(summt / count);
			edge.setAvgsdt(sumdst / count);
		}
		return serEdgeList;

	}
}