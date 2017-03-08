package wsc.ecj.gp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPProblem;
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
		
		
		 init.mutationTimess++;
		 if(init.mutationTimess == 12){
		 System.out.println("degbug entry~"+init.mutationTimess);
		 }
		 System.out.println("mutation: TIMES~"+init.mutationTimess);
		

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
//			 System.out.println("old tree:"+tree.toString());;

			// Randomly select a node in the tree to be mutation
			List<GPNode> allNodes = tree.getFiltedTreeNodes();
			int selectedIndex = WSCInitializer.random.nextInt(allNodes.size());
			GPNode selectedNode = allNodes.get(selectedIndex);
			InOutNode ioNode = (InOutNode) selectedNode;


			// obtain the target Services of selected Node, if the selected Node
			// is a service node and put in a map
//			Map<String, String> Inst2TargetSerMap = new HashMap<String, String>();
//			if (selectedNode instanceof ServiceGPNode) {
//				List<ServiceEdge> outgoingEdgeOfSelectNode = ((ServiceGPNode) selectedNode).getSemanticEdges();
//				for (ServiceEdge serEdge : outgoingEdgeOfSelectNode) {
//					for (ParamterConn pConn : serEdge.getpConnList()) {
//						String outputInst = pConn.getOutputInst();
//						String targetSerId = pConn.getTargetServiceID();
//						if (outputInst == null || outputInst.equals("") || targetSerId == null
//								|| targetSerId.equals("")) {
//							System.err.println("outputInst or outTargetSerId is NULL");
//						}
////						System.err.println("outputInst or outTargetSerId is "+ outputInst+"->"+targetSerId );
//						Inst2TargetSerMap.put(outputInst, targetSerId);
//					}
//
//				}
//			}

			// Combine the input from the node with the overall task input, as
			// the latter is available from anywhere

			Set<String> combinedInputs = new HashSet<String>();
			Set<String> combinedoutputs = new HashSet<String>();

			for (ServiceInput iNode : ioNode.getInputs()) {
				combinedInputs.add(iNode.getInput());
			}

			for (String tskInp : WSCInitializer.taskInput) {
				if (!combinedInputs.contains(tskInp)) {
					combinedInputs.add(tskInp);
				}

			}

			//required outputs

			for (ServiceOutput oNode : ioNode.getOutputs()) {
				combinedoutputs.add(oNode.getOutput());
			}

			// Generate a new tree based on the input/output information of the
			// current node

			List<String> Inputs4Node = new ArrayList<String>(combinedInputs);
			List<String> outputs4Node = new ArrayList<String>(combinedoutputs);

			// System.out.println("selected: "+selectedNode);
			//
			// for (String input :Inputs4Node) {
			// System.out.print("I:"+input+"; ");
			// }
			// System.out.println("");
			//
			// for (String output :outputs4Node) {
			// System.out.print("O:"+output+"; ");
			// }
			// System.out.println("");

//			ServiceGraph graph4Mutation = species.Graph4Mutation(init, Inputs4Node, outputs4Node, Inst2TargetSerMap);
			ServiceGraph graph4Mutation = species.Graph4Mutation(init, Inputs4Node, outputs4Node);


//			 System.out.println(" @mutation graph:"+graph4Mutation.toString());;

			// GPNode tree4Mutation = species.toTree4Mutation("startNode",
			// graph4Mutation);
			GPNode tree4Mutation = species.toSemanticTree("startNode", graph4Mutation);
//			System.out.println(tree4Mutation.toString());

			// Replace the old tree with the new one
			tree.replaceNode4Mutation(selectedNode, tree4Mutation);
			
			
			 System.out.println("new mutation tree:"+tree.toString());;
//			 System.out.println("_________________________________________________________________________________________________________");

			 
			 
			 
			tree.evaluated = false;

		}
		return n;
	}

}
