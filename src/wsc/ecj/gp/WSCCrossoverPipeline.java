package wsc.ecj.gp;

import java.util.Collections;
import java.util.List;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPNode;
import ec.util.Parameter;
import wsc.data.pool.SemanticsPool;
import wsc.graph.ParamterConn;
import wsc.graph.ServiceInput;
import wsc.graph.ServiceOutput;

public class WSCCrossoverPipeline extends BreedingPipeline {

	private static final long serialVersionUID = 1L;

	@Override
	public Parameter defaultBase() {
		return new Parameter("wsccrossoverpipeline");
	}

	@Override
	public int numSources() {
		return 2;
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation, Individual[] inds, EvolutionState state,
			int thread) {

		WSCInitializer init = (WSCInitializer) state.initializer;

		Individual[] inds1 = new Individual[inds.length];
		Individual[] inds2 = new Individual[inds.length];

		int n1 = sources[0].produce(min, max, 0, subpopulation, inds1, state, thread);
		int n2 = sources[1].produce(min, max, 0, subpopulation, inds2, state, thread);

		if (!(sources[0] instanceof BreedingPipeline)) {
			for (int q = 0; q < n1; q++)
				inds1[q] = (Individual) (inds1[q].clone());
		}

		if (!(sources[1] instanceof BreedingPipeline)) {
			for (int q = 0; q < n2; q++)
				inds2[q] = (Individual) (inds2[q].clone());
		}

		if (!(inds1[0] instanceof WSCIndividual))
			// uh oh, wrong kind of individual
			state.output
					.fatal("WSCCrossoverPipeline didn't get a WSCIndividual. The offending individual is in subpopulation "
							+ subpopulation + " and it's:" + inds1[0]);

		if (!(inds2[0] instanceof WSCIndividual))
			// uh oh, wrong kind of individual
			state.output
					.fatal("WSCCrossoverPipeline didn't get a WSCIndividual. The offending individual is in subpopulation "
							+ subpopulation + " and it's:" + inds2[0]);

		int nMin = Math.min(n1, n2);

		// Perform crossover
		for (int q = start, x = 0; q < nMin + start; q++, x++) {
			WSCIndividual t1 = ((WSCIndividual) inds1[x]);
			WSCIndividual t2 = ((WSCIndividual) inds2[x]);

			// Find all nodes from both candidates
			List<GPNode> allT1Nodes = t1.getFiltedTreeNodes();
			List<GPNode> allT2Nodes = t2.getFiltedTreeNodes();

			// Test whether constains startNode or endNodes;
			// for (GPNode gpNode : allT1Nodes) {
			// if (gpNode instanceof ServiceGPNode) {
			// ServiceGPNode sgp = (ServiceGPNode) gpNode;
			// if (sgp.getSerName().equals("startNode")) {
			// // initial variable rootNode
			// System.out.println("contains startNode");
			// }
			// if (sgp.getSerName().equals("endNode")) {
			// // remove endNode
			// System.out.println("contains endNode");
			// }
			//
			// }
			// }

			// Shuffle them so that the crossover is random
			Collections.shuffle(allT1Nodes, WSCInitializer.random);
			Collections.shuffle(allT2Nodes, WSCInitializer.random);

			// For each t1 node, see if it can be replaced by a t2 node
			GPNode[] nodes = findReplacement(init, allT1Nodes, allT2Nodes);
			GPNode nodeT1 = nodes[0];
			GPNode replacementT2 = nodes[1];
//			state.output.println(" -----------replace part from A:"+nodeT1, 0);
//			state.output.println(" -----------replace part from B:"+replacementT2, 0);


			// For each t2 node, see if it can be replaced by a t1 node
			// nodes = findReplacement(init, allT2Nodes, allT1Nodes);
			// GPNode nodeT2 = nodes[0];
			// GPNode replacementT1 = nodes[1];

			// Perform replacement in both individuals
			t1.replaceNode4Crossover(nodeT1, replacementT2);
			t2.replaceNode4Crossover(replacementT2, nodeT1);
			// t2.replaceNode(nodeT2, replacementT1);

			inds[q] = t1;
			inds[q].evaluated = false;

			if (q + 1 < inds.length) {
				inds[q + 1] = t2;
				inds[q + 1].evaluated = false;
			}
//			state.output.println(" CROSSOVER !!!!!!!", 0);
//			state.output.println(" new Individual:"+t1.toString(), 0);
//			state.output.println(" new Individual:"+t2.toString(), 0);
//			state.output.println(" XXXXXXXXXXXXXXXXXXXXx"+t2.toString(), 0);



		}
		return n1;

	}

	public GPNode[] findReplacement(WSCInitializer init, List<GPNode> nodes, List<GPNode> replacements) {
		GPNode[] result = new GPNode[2];
		outterLoop: for (GPNode node : nodes) {
			for (GPNode replacement : replacements) {
				/*
				 * Check if the inputs of replacement are subsumed by the inputs
				 * of the node and the outputs of the node are subsumed by the
				 * outputs of the replacement. This will ensure that the
				 * replacement has equivalent functionality to the replacement.
				 */
				InOutNode ioNode = (InOutNode) node;
				InOutNode ioReplacement = (InOutNode) replacement;
				if (IsReplacementFound(init, ioNode, ioReplacement)) {
//					System.out.println("selected Node ******" + ioNode.toString());
//					System.out.println("replaced Node ******" + ioReplacement.toString());
					result[0] = node;
					result[1] = replacement;
					break outterLoop;
				}
			}
		}
		return result;
	}

	// check both the inputs and outputs from node and replacement node are
	// matched.

	private boolean IsReplacementFound(WSCInitializer init, InOutNode ioNode, InOutNode ioReplacement) {
		boolean isInputFound = false;
		boolean isOutputFound = false;
		// if (ioNode.getInputs() == null) {
		// System.out.println("toString"+ioNode.toString());
		//
		// System.out.println("NULLLLLLLLLLLLLLLLL");
		// }
		isInputFound = searchReplacement4Inputs(init.initialWSCPool.getSemanticsPool(), ioNode.getInputs(),
				ioReplacement.getInputs());
		isOutputFound = searchReplacement4Outputs(init.initialWSCPool.getSemanticsPool(), ioNode.getOutputs(),
				ioReplacement.getOutputs());

		return isInputFound && isOutputFound;

	}

	/**
	 * search input functional correctness for replacements
	 *
	 * @param semanticsPool
	 * @param graphOutputSetMap
	 * @param intputList
	 * @return boolean
	 */
	// public boolean searchReplacement(SemanticsPool semanticsPool,
	// HashSet<String> inputSet, HashSet<String> inputList) {
	public boolean searchReplacement4Inputs(SemanticsPool semanticsPool, List<ServiceInput> ioNodeInputs,
			List<ServiceInput> ioReplacement) {

		for (ServiceInput serInput : ioNodeInputs) {
			serInput.setSatified(false);
		}
		for (ServiceInput serInput : ioReplacement) {
			serInput.setSatified(false);
		}

		if (ioNodeInputs.size() == ioReplacement.size()) {
			int relevantServiceCount = 0;
			for (int i = 0; i < ioReplacement.size(); i++) {
				String giveninput = ioReplacement.get(i).getInput();
				for (int j = 0; j < ioNodeInputs.size(); j++) {
					ServiceInput serInput = ioNodeInputs.get(j);
					if (!serInput.isSatified()) {
						String existInput = ioNodeInputs.get(j).getInput();
						ParamterConn pConn = semanticsPool.searchSemanticMatchFromInst(giveninput, existInput);
						boolean foundmatched = pConn.isConsidered();
						if (foundmatched) {
							serInput.setSatified(true);
							break;// each inst can only be used for one time
						}

					}

				}

			}

			for (ServiceInput sInput : ioNodeInputs) {
				boolean sf = sInput.isSatified();
				if (sf == true) {
					relevantServiceCount++;
				}
			}

			if (relevantServiceCount == ioNodeInputs.size()) {
				return true;
			}

		}

		return false;
	}

	/**
	 * search input functional correctness for replacements
	 *
	 * @param semanticsPool
	 * @param graphOutputSetMap
	 * @param intputList
	 * @return boolean
	 */
	// public boolean searchReplacement(SemanticsPool semanticsPool,
	// HashSet<String> inputSet, HashSet<String> inputList) {
	public boolean searchReplacement4Outputs(SemanticsPool semanticsPool, List<ServiceOutput> ioNodeOutputs,
			List<ServiceOutput> ioReplacement) {
		for (ServiceOutput serInput : ioNodeOutputs) {
			serInput.setSatified(false);
		}
		for (ServiceOutput serInput : ioReplacement) {
			serInput.setSatified(false);
		}


		if (ioNodeOutputs.size() == ioReplacement.size()) {
			int relevantServiceCount = 0;
			for (int i = 0; i < ioReplacement.size(); i++) {
				String givenOutput = ioReplacement.get(i).getOutput();
				for (int j = 0; j < ioNodeOutputs.size(); j++) {
					ServiceOutput serOutput = ioNodeOutputs.get(j);
					if (!serOutput.isSatified()) {
						String existOutput = ioNodeOutputs.get(j).getOutput();
						ParamterConn pConn = semanticsPool.searchSemanticMatchFromInst(givenOutput, existOutput);
						boolean foundmatched = pConn.isConsidered();
						if (foundmatched) {
							serOutput.setSatified(true);
							break;// each inst can only be used for one time
						}

					}

				}

			}

			for (ServiceOutput sInput : ioNodeOutputs) {
				boolean sf = sInput.isSatified();
				if (sf == true) {
					relevantServiceCount++;
				}
			}

			if (relevantServiceCount == ioNodeOutputs.size()) {
				return true;
			}
		}

		return false;
	}

}
