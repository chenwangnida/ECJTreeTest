package ec.pso;

import java.util.Arrays;
import java.util.Random;

public class Particle {
	private String strRepresentation;
	private double availability;
	private double reliability;
	private double time;
	private double cost;
	private double matchingType;
	private double semanticDistance;

	public double fitness = 0.0; // The higher, the fitterSSSS
	public float[] dimensions = new float[GraphPSO.numDimensions];
	public float[] velocity = new float[GraphPSO.numDimensions];
	// personal best values
	public double bestFitness = Double.NEGATIVE_INFINITY;
	public float[] bestDimensions = new float[GraphPSO.numDimensions];

	// global best values
	public static double globalBestFitness = Double.NEGATIVE_INFINITY;
	public static float[] globalBestDimensions = new float[GraphPSO.numDimensions];
	public static String globalGraphString;
	public static double globalBestAvailability;
	public static double globalBestReliability;
	public static double globalBestTime;
	public static double globalBestCost;
	public static double globalBestMatchingType;
	public static double globalBestSemanticDistance;

	public String getStrRepresentation() {
		return strRepresentation;
	}

	public void setStrRepresentation(String strRepresentation) {
		this.strRepresentation = strRepresentation;
	}

	public double getAvailability() {
		return availability;
	}

	public void setAvailability(double availability) {
		this.availability = availability;
	}

	public double getReliability() {
		return reliability;
	}

	public void setReliability(double reliability) {
		this.reliability = reliability;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double times) {
		this.time = times;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public double getSemanticDistance() {
		return semanticDistance;
	}

	public void setSemanticDistance(double semanticDistance) {
		this.semanticDistance = semanticDistance;
	}

	public double getMatchingType() {
		return matchingType;
	}

	public void setMatchingType(double matchingType) {
		this.matchingType = matchingType;
	}

	/**
	 * Creates a particle with null dimensions.
	 *
	 * @param serviceMap
	 */
	public Particle(Random random) {
		for (int i = 0; i < dimensions.length; i++) {
			dimensions[i] = random.nextFloat();
		}
		Arrays.fill(bestDimensions, 0.0f);
		Arrays.fill(globalBestDimensions, 0.0f);
	}

	/**
	 * Resets static fields.
	 */
	public static void reset() {
		// global best values
		globalBestFitness = Double.NEGATIVE_INFINITY;
		globalBestDimensions = new float[GraphPSO.numDimensions];
		globalGraphString = null;
	}

	/**
	 * Checks if the current solution is fitter than the overall personal best.
	 * If it is, set personal best as values from current solution.
	 */
	public void updatePersonalBest() {
		if (fitness > bestFitness) {
			bestFitness = fitness;
			bestDimensions = Arrays.copyOf(dimensions, dimensions.length);
		}
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return strRepresentation;
	}

}
