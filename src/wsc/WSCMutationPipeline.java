package wsc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPNode;
import ec.util.Parameter;
import graph.Graph;
import graph.GraphNode;

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
	public int produce(int min, int max, int start, int subpopulation,
			Individual[] inds, EvolutionState state, int thread) {
		WSCInitializer init = (WSCInitializer) state.initializer;

		int n = sources[0].produce(min, max, start, subpopulation, inds, state, thread);

        if (!(sources[0] instanceof BreedingPipeline)) {
            for(int q=start;q<n+start;q++)
                inds[q] = (Individual)(inds[q].clone());
        }

        if (!(inds[start] instanceof WSCIndividual))
            // uh oh, wrong kind of individual
            state.output.fatal("WSCMutationPipeline didn't get a WSCIndividual. The offending individual is in subpopulation "
            + subpopulation + " and it's:" + inds[start]);

        // Perform mutation
        for(int q=start;q<n+start;q++) {
            WSCIndividual tree = (WSCIndividual)inds[q];
            WSCSpecies species = (WSCSpecies) tree.species;

            // Randomly select a node in the tree to be mutation
            List<GPNode> allNodes = new ArrayList<GPNode>();
            Queue<GPNode> queue = new LinkedList<GPNode>();

            queue.offer(tree.trees[0].child);

            while(!queue.isEmpty()) {
                GPNode current = queue.poll();
                allNodes.add(current);
                if (current.children != null) {
                    for (GPNode child : current.children)
                        allNodes.add( child );
                }
            }

            int selectedIndex = init.random.nextInt(allNodes.size());
            GPNode selectedNode = allNodes.get( selectedIndex );
            InOutNode ioNode = (InOutNode) selectedNode;

            // Combine the input from the node with the overall task input, as the latter is available from anywhere
            Set<String> combinedInputs = new HashSet<String>();
            combinedInputs.addAll( init.taskInput );
            combinedInputs.addAll( ioNode.getInputs() );

            // Generate a new tree based on the input/output information of the current node
            //GPNode newNode = species.createNewTree( state, combinedInputs, ioNode.getOutputs() ); //XXX

            double[] mockQoS = new double[4];
            mockQoS[WSCInitializer.TIME] = 0.0;
            mockQoS[WSCInitializer.COST] = 0.0;
            mockQoS[WSCInitializer.AVAILABILITY] = 1.0;
            mockQoS[WSCInitializer.RELIABILITY] = 1.0;
            Service startNode = new Service("start", mockQoS, new HashSet<String>(), combinedInputs);
            Service endNode = new Service("end", mockQoS, ioNode.getOutputs(), new HashSet<String>());
            Graph newGraph = species.createNewGraph(state, startNode, endNode, init.relevant);
            GPNode newNode = newGraph.nodeMap.get("start").toTree();

            // Replace the old tree with the new one
            tree.replaceNode( selectedNode, newNode );
            tree.evaluated=false;
        }
        return n;
	}

}
