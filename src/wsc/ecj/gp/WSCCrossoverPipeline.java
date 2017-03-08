package wsc.ecj.gp;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
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

			// Shuffle them so that the crossover is random
			Collections.shuffle(allT1Nodes, WSCInitializer.random);
			Collections.shuffle(allT2Nodes, WSCInitializer.random);

			// For each t1 node, see if it can be replaced by a t2 node
			GPNode[] nodes = findReplacement4ExactMatch(init, allT1Nodes, allT2Nodes);

			GPNode nodeT1 = nodes[0];
			GPNode replacementT2 = nodes[1];

//			if (nodeT1 != null && replacementT2 != null) {

				t1.replaceNode4Crossover1(nodeT1, replacementT2);

//				 state.output.println(" new A:"+t1.toString(), 0);

				t2.replaceNode4Crossover1(replacementT2, nodeT1);

//				 state.output.println(" new B:"+t2.toString(), 0);
//			} else {

				// if there is no exact match for swap, then check the
				// multiple-function swap for crossover.
//				GPNode[] nodes4Function1 = findReplacement4SemanticMatch(init, allT1Nodes, allT2Nodes);
//
//				GPNode node4Function1T1 = nodes4Function1[0];
//				GPNode replacement4Function1T1 = nodes4Function1[1];

//				GPNode[] nodes4Function2 = findReplacement4SemanticMatch(init, allT2Nodes, allT1Nodes);
//				GPNode node4Function2T1 = nodes4Function2[0];
//				GPNode replacement4Function2T2 = nodes4Function2[1];

//				t1.replaceNode4Crossover(node4Function1T1, replacement4Function1T1);
//				t2.replaceNode4Crossover1(node4Function2T1, replacement4Function2T2);

//			}

			inds[q] = t1;
			inds[q].evaluated = false;

			if (q + 1 < inds.length) {
				inds[q + 1] = t2;
				inds[q + 1].evaluated = false;
			}
			// state.output.println(" CROSSOVER !!!!!!!", 0);
			// state.output.println(" -----------next
			// crossover---------------------", 0);

		}

		// init.crossoverTimess++;
		// if(init.crossoverTimess == 98){
		// System.out.println("degbug entry~"+init.crossoverTimess);
		//
		//
		// }
		// System.out.println("crossover: TIMES~"+init.crossoverTimess);

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
					// System.out.println("selected Node ******" +
					// ioNode.toString());
					// System.out.println("replaced Node ******" +
					// ioReplacement.toString());
					result[0] = node;
					result[1] = replacement;
					break outterLoop;
				}
			}
		}
		return result;
	}

	public GPNode[] findReplacement4ExactMatch(WSCInitializer init, List<GPNode> nodes, List<GPNode> replacements) {
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
				if (IsReplacementFound4ExactMatch(init, ioNode, ioReplacement)) {
					// System.out.println("selected Node ******" +
					// ioNode.toString());
					// System.out.println("replaced Node ******" +
					// ioReplacement.toString());
					result[0] = node;
					result[1] = replacement;
					break outterLoop;
				}
			}
		}
		return result;
	}

	public GPNode[] findReplacement4SemanticMatch(WSCInitializer init, List<GPNode> nodes, List<GPNode> replacements) {
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
				if (IsReplacementFound4SemanticMatch(init, ioNode, ioReplacement)) {
					// System.out.println("selected Node ******" +
					// ioNode.toString());
					// System.out.println("replaced Node ******" +
					// ioReplacement.toString());
					result[0] = node;
					result[1] = replacement;
					break outterLoop;
				}
			}
		}
		return result;
	}

	public GPNode[] findReplacement(WSCInitializer init, List<GPNode> nodes, List<GPNode> replacements,
			BiMap<String, String> inst1Toinst2) {
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
				if (IsReplacementFound(init, ioNode, ioReplacement, inst1Toinst2)) {
					// System.out.println("selected Node ******" +
					// ioNode.toString());
					// System.out.println("replaced Node ******" +
					// ioReplacement.toString());
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

	private boolean IsReplacementFound(WSCInitializer init, InOutNode ioNode, InOutNode ioReplacement,
			BiMap<String, String> inst1Toinst2) {
		boolean isInputFound = false;
		boolean isOutputFound = false;
		// if (ioNode.getInputs() == null) {
		// System.out.println("toString"+ioNode.toString());
		//
		// System.out.println("NULLLLLLLLLLLLLLLLL");
		// }
		isInputFound = searchReplacement4Inputs(WSCInitializer.initialWSCPool.getSemanticsPool(), ioNode.getInputs(),
				ioReplacement.getInputs());
		isOutputFound = searchReplacement4Outputs(WSCInitializer.initialWSCPool.getSemanticsPool(), ioNode.getOutputs(),
				ioReplacement.getOutputs());

		if (isInputFound && isOutputFound) {
			Replacement4Outputs(WSCInitializer.initialWSCPool.getSemanticsPool(), ioNode.getOutputs(),
					ioReplacement.getOutputs(), inst1Toinst2);
		}

		return isInputFound && isOutputFound;

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
		isInputFound = searchReplacement4Inputs(WSCInitializer.initialWSCPool.getSemanticsPool(), ioNode.getInputs(),
				ioReplacement.getInputs());
		isOutputFound = searchReplacement4Outputs(WSCInitializer.initialWSCPool.getSemanticsPool(), ioNode.getOutputs(),
				ioReplacement.getOutputs());

		return isInputFound && isOutputFound;

	}

	// check both the inputs and outputs from node and replacement node are
	// matched.

	private boolean IsReplacementFound4SemanticMatch(WSCInitializer init, InOutNode ioNode, InOutNode ioReplacement) {
		boolean isInputFound = false;
		boolean isOutputFound = false;
		// if (ioNode.getInputs() == null) {
		// System.out.println("toString"+ioNode.toString());
		//
		// System.out.println("NULLLLLLLLLLLLLLLLL"); 
		// }
		isInputFound = searchReplacement4Inputs4SubsumeMatch(WSCInitializer.initialWSCPool.getSemanticsPool(),
				ioNode.getInputs(), ioReplacement.getInputs());
		isOutputFound = searchReplacement4Outputs4PluginMatch(WSCInitializer.initialWSCPool.getSemanticsPool(),
				ioNode.getOutputs(), ioReplacement.getOutputs());

		return isInputFound && isOutputFound;

	}
	// check both the inputs and outputs from node and replacement node are
	// matched.

	private boolean IsReplacementFound4ExactMatch(WSCInitializer init, InOutNode ioNode, InOutNode ioReplacement) {
		boolean isInputFound = false;
		boolean isOutputFound = false;
		// if (ioNode.getInputs() == null) {
		// System.out.println("toString"+ioNode.toString());
		//
		// System.out.println("NULLLLLLLLLLLLLLLLL");
		// }
		isInputFound = searchReplacement4Inputs4ExactMatch(WSCInitializer.initialWSCPool.getSemanticsPool(),
				ioNode.getInputs(), ioReplacement.getInputs());
		isOutputFound = searchReplacement4Outputs4ExactMatch(WSCInitializer.initialWSCPool.getSemanticsPool(),
				ioNode.getOutputs(), ioReplacement.getOutputs());

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

	public boolean searchReplacement4Inputs4ExactMatch(SemanticsPool semanticsPool, List<ServiceInput> ioNodeInputs0,
			List<ServiceInput> ioReplacement0) {
	
		List<ServiceInput> ioNodeInputs = ioNodeInputs0.stream().distinct().collect(Collectors.toList());
		List<ServiceInput> ioReplacement = ioReplacement0.stream().distinct().collect(Collectors.toList());

		
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
						ParamterConn pConn = semanticsPool.searchSemanticMatchFromInst4ExactMatch(giveninput,
								existInput);
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

	public boolean searchReplacement4Inputs4SubsumeMatch(SemanticsPool semanticsPool, List<ServiceInput> ioNodeInputs0,
			List<ServiceInput> ioReplacement0) {
		
		List<ServiceInput> ioNodeInputs = ioNodeInputs0.stream().distinct().collect(Collectors.toList());
		List<ServiceInput> ioReplacement = ioReplacement0.stream().distinct().collect(Collectors.toList());

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
						ParamterConn pConn = semanticsPool.searchSemanticMatchFromInst4SubsumeMatch(giveninput,
								existInput);
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

	public boolean searchReplacement4Outputs4ExactMatch(SemanticsPool semanticsPool, List<ServiceOutput> ioNodeOutputs0,
			List<ServiceOutput> ioReplacement0) {
		
		List<ServiceOutput> ioNodeOutputs = ioNodeOutputs0.stream().distinct().collect(Collectors.toList());
		List<ServiceOutput> ioReplacement = ioReplacement0.stream().distinct().collect(Collectors.toList());
		
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
						ParamterConn pConn = semanticsPool.searchSemanticMatchFromInst4ExactMatch(givenOutput,
								existOutput);
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
	
	public boolean searchReplacement4Outputs4PluginMatch(SemanticsPool semanticsPool, List<ServiceOutput> ioNodeOutputs0,
			List<ServiceOutput> ioReplacement0) {
		
		List<ServiceOutput> ioNodeOutputs = ioNodeOutputs0.stream().distinct().collect(Collectors.toList());
		List<ServiceOutput> ioReplacement = ioReplacement0.stream().distinct().collect(Collectors.toList());
		
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
						ParamterConn pConn = semanticsPool.searchSemanticMatchFromInst4PluginMatch(givenOutput,
								existOutput);
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

	// public boolean searchReplacement(SemanticsPool semanticsPool,
	// HashSet<String> inputSet, HashSet<String> inputList) {
	public BiMap<String, String> Replacement4Outputs(SemanticsPool semanticsPool, List<ServiceOutput> ioNodeOutputs,
			List<ServiceOutput> ioReplacement, BiMap<String, String> inst1Toinst2) {
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
							inst1Toinst2.put(existOutput, givenOutput);
							break;// each inst can only be used for one time
						}

					}

				}

			}
		}

		return inst1Toinst2;
	}

}
