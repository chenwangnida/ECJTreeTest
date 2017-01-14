package wsc.ecj.gp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPNode;
import ec.util.Parameter;
import wsc.graph.ServiceGraph;
import wsc.graph.ServiceInput;
import wsc.graph.ServiceOutput;

public class WSCMutationPipeline extends BreedingPipeline {

	private static final long serialVersionUID = 1L;

	@Override
	public Parameter defaultBase() {
		return new Parameter("wscmutationpipeline");
	}

	@Override
	public int numSources() {
		return 1;
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation, Individual[] inds, EvolutionState state,
			int thread) {
		WSCInitializer init = (WSCInitializer) state.initializer;

		int n = sources[0].produce(min, max, start, subpopulation, inds, state, thread);

		if (!(sources[0] instanceof BreedingPipeline)) {
			for (int q = start; q < n + start; q++)
				inds[q] = (Individual) (inds[q].clone());
		}

		if (!(inds[start] instanceof WSCIndividual))
			// uh oh, wrong kind of individual
			state.output
					.fatal("WSCMutationPipeline didn't get a WSCIndividual. The offending individual is in subpopulation "
							+ subpopulation + " and it's:" + inds[start]);

		// Perform mutation
		for (int q = start; q < n + start; q++) {
			WSCIndividual tree = (WSCIndividual) inds[q];
			WSCSpecies species = (WSCSpecies) tree.species;

			// Randomly select a node in the tree to be mutation
			List<GPNode> allNodes = tree.getFiltedTreeNodes();
			int selectedIndex = init.random.nextInt(allNodes.size());
			GPNode selectedNode = allNodes.get(selectedIndex);
			InOutNode ioNode = (InOutNode) selectedNode;

			// Combine the input from the node with the overall task input, as
			// the latter is available from anywhere

			Set<String> combinedInputs = new HashSet<String>();
			Set<String> combinedoutputs = new HashSet<String>();

			for (ServiceInput iNode :ioNode.getInputs()) {
				combinedInputs.add(iNode.getInput());
			}

			for(String tskInp :init.taskInput){
				if(!combinedInputs.contains(tskInp)){
					combinedInputs.add(tskInp);
				}

			}

			for (ServiceOutput oNode :ioNode.getOutputs()) {
				combinedoutputs.add(oNode.getOutput());
			}

			// Generate a new tree based on the input/output information of the
			// current node
			
			List<String> Inputs4Node = new ArrayList(combinedInputs);
			List<String> outputs4Node = new ArrayList(combinedoutputs);

			System.out.println("selected: "+selectedNode);
			
			for (String input :Inputs4Node) {
				System.out.print("I:"+input+"; ");
			}
			System.out.println("");

			for (String output :outputs4Node) {
				System.out.print("O:"+output+"; ");
			}
			System.out.println("");

			ServiceGraph graph4Mutation = species.Graph4Mutation(init, Inputs4Node, outputs4Node);
	
			
			System.out.println(" @mutation graph:"+graph4Mutation.toString());;

//			GPNode tree4Mutation = species.toTree4Mutation("startNode", graph4Mutation);
			GPNode tree4Mutation = species.toWeightedTree("startNode", graph4Mutation);
			System.out.println("mutation graph 4tree"+tree4Mutation.toString());;
			System.out.println("_________________________________________________________________________________________________________");

			// Replace the old tree with the new one
			tree.replaceNode4Mutation(selectedNode, tree4Mutation);
			tree.evaluated = false;
		}
		return n;
	}

}
