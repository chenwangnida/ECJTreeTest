package wsc.ecj.gp;

import ec.util.*;
import ec.*;
import ec.gp.*;
import ec.simple.*;
import wsc.data.pool.Service;
import wsc.graph.ServiceEdge;

public class WSC extends GPProblem implements SimpleProblemForm {

	private static final long serialVersionUID = 1L;

	public void setup(final EvolutionState state, final Parameter base) {
		// very important, remember this
		super.setup(state, base);

		// verify our input is the right class (or subclasses from it)
		if (!(input instanceof WSCData))
			state.output.fatal("GPData class must subclass from " + WSCData.class, base.push(P_DATA), null);
	}

	public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
			final int threadnum) {
		if (!ind.evaluated) {
			WSCInitializer init = (WSCInitializer) state.initializer;
			WSCData input = (WSCData) (this.input);

			GPIndividual gpInd = (GPIndividual) ind;

//			state.output.println("Evaluate new Individual:"+gpInd.toString(), 0);


			gpInd.trees[0].child.eval(state, threadnum, input, stack, ((GPIndividual) ind), this);
			double[] qos = new double[4];
			qos[WSCInitializer.TIME] = input.maxTime;
			qos[WSCInitializer.AVAILABILITY] = 1.0;
			qos[WSCInitializer.RELIABILITY] = 1.0;

			double mt = 1.0;
			double dst = 0.0; // Exact Match dst = 1 ;
			for (ServiceEdge semanticQuality : input.semanticEdges) {
				mt *= semanticQuality.getAvgmt();
				dst += semanticQuality.getAvgsdt();

			}

			dst = dst/(input.semanticEdges.size());
//			System.out.println("semantic edge Size :"+ input.semanticEdges.size());

			for (Service s : input.seenServices) {
				qos[WSCInitializer.COST] += s.qos[WSCInitializer.COST];
				qos[WSCInitializer.AVAILABILITY] *= s.qos[WSCInitializer.AVAILABILITY];
				qos[WSCInitializer.RELIABILITY] *= s.qos[WSCInitializer.RELIABILITY];
			}

			double fitness = calculateFitness(qos[WSCInitializer.AVAILABILITY], qos[WSCInitializer.RELIABILITY],
					qos[WSCInitializer.TIME], qos[WSCInitializer.COST], mt, dst, init);

			// the fitness better be SimpleFitness!
			SimpleFitness f = ((SimpleFitness) ind.fitness);
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
}