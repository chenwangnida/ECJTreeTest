package wsc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ec.EvolutionState;
import ec.Individual;
import ec.Species;
import ec.gp.GPNode;
import ec.util.Parameter;
import graph.Graph;
import graph.GraphEdge;
import graph.GraphNode;

public class WSCSpecies extends Species {

	private static final long serialVersionUID = 1L;

	@Override
	public Parameter defaultBase() {
		return new Parameter("wscspecies");
	}

	@Override
	public Individual newIndividual(EvolutionState state, int thread) {
	    WSCInitializer init = (WSCInitializer) state.initializer;

	    Graph graph = createNewGraph(state, init.startServ, init.endServ, init.relevant);
	    // Turn graph to tree, and return that tree
	    GPNode treeRoot = graph.nodeMap.get("start").toTree();

	    //GPNode treeRoot = createNewTree(state, init.taskInput, init.taskOutput); // XXX

	    WSCIndividual tree = new WSCIndividual(treeRoot);

	    System.out.println("Create tree");
	    try {
	    	FileWriter writer = new FileWriter(new File("debug-graph.dot"));
			writer.append(graph.toString());
			writer.close();
			FileWriter writer2 = new FileWriter(new File("debug-tree.dot"));
			writer2.append(tree.toString());
			writer2.close();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

	    return tree;
	}

	public Graph createNewGraph(EvolutionState state, Service start, Service end, Set<Service> relevant) {
		GraphNode startNode = new GraphNode(start);
		GraphNode endNode = new GraphNode(end);

		WSCInitializer init = (WSCInitializer) state.initializer;

		Graph newGraph = new Graph();

		Set<String> currentEndInputs = new HashSet<String>();
		Map<String,GraphEdge> connections = new HashMap<String,GraphEdge>();

		// Connect start node
		connectCandidateToGraphByInputs(startNode, connections, newGraph, currentEndInputs, init);

		Set<Service> seenNodes = new HashSet<Service>();
		List<Service> candidateList = new ArrayList<Service>();

		addToCandidateList(start, seenNodes, relevant, candidateList, init);

		Collections.shuffle(candidateList, init.random);

		finishConstructingGraph(currentEndInputs, endNode, candidateList, connections, init, newGraph, seenNodes, relevant);

		return newGraph;
	}

	public void finishConstructingGraph(Set<String> currentEndInputs, GraphNode end, List<Service> candidateList, Map<String,GraphEdge> connections,
	        WSCInitializer init, Graph newGraph, Set<Service> seenNodes, Set<Service> relevant) {

		// While end cannot be connected to graph
		while(!checkCandidateNodeSatisfied(init, connections, newGraph, end, end.getInputs(), null)){
			connections.clear();

            // Select node
            int index;

            candidateLoop:
            for (index = 0; index < candidateList.size(); index++) {
                Service candidate = candidateList.get(index);
                // For all of the candidate inputs, check that there is a service already in the graph
                // that can satisfy it

                GraphNode candNode = new GraphNode(candidate);
                if (!checkCandidateNodeSatisfied(init, connections, newGraph, candNode, candidate.getInputs(), null)) {
                    connections.clear();
                	continue candidateLoop;
                }

                // Connect candidate to graph, adding its reachable services to the candidate list
                connectCandidateToGraphByInputs(candNode, connections, newGraph, currentEndInputs, init);
                connections.clear();

                addToCandidateList(candidate, seenNodes, relevant, candidateList, init);

                break;
            }

            candidateList.remove(index);
            Collections.shuffle(candidateList, init.random);
        }

        connectCandidateToGraphByInputs(end, connections, newGraph, currentEndInputs, init);
        connections.clear();
        init.removeDanglingNodes(newGraph);
	}

	private boolean checkCandidateNodeSatisfied(WSCInitializer init,
			Map<String, GraphEdge> connections, Graph newGraph,
			GraphNode candidate, Set<String> candInputs, Set<GraphNode> fromNodes) {

		Set<String> candidateInputs = new HashSet<String>(candInputs);
		Set<String> startIntersect = new HashSet<String>();

		// Check if the start node should be considered
		GraphNode start = newGraph.nodeMap.get("start");

		if (fromNodes == null || fromNodes.contains(start)) {
    		for(String output : start.getOutputs()) {
    			Set<String> inputVals = init.taxonomyMap.get(output).servicesWithInput.get(candidate.getService());
    			if (inputVals != null) {
    				candidateInputs.removeAll(inputVals);
    				startIntersect.addAll(inputVals);
    			}
    		}

    		if (!startIntersect.isEmpty()) {
    			GraphEdge startEdge = new GraphEdge(startIntersect);
    			startEdge.setFromNode(start);
    			startEdge.setToNode(candidate);
    			connections.put(start.getName(), startEdge);
    		}
		}


		for (String input : candidateInputs) {
			boolean found = false;
			for (Service s : init.taxonomyMap.get(input).servicesWithOutput) {
			    if (fromNodes == null || fromNodes.contains(s)) {
    				if (newGraph.nodeMap.containsKey(s.getName())) {
    					Set<String> intersect = new HashSet<String>();
    					intersect.add(input);

    					GraphEdge mapEdge = connections.get(s.getName());
    					if (mapEdge == null) {
    						GraphEdge e = new GraphEdge(intersect);
    						e.setFromNode(newGraph.nodeMap.get(s.getName()));
    						e.setToNode(candidate);
    						connections.put(e.getFromNode().getName(), e);
    					} else
    						mapEdge.getIntersect().addAll(intersect);

    					found = true;
    					break;
    				}
			    }
			}
			// If that input cannot be satisfied, move on to another candidate
			// node to connect
			if (!found) {
				// Move on to another candidate
				return false;
			}
		}
		return true;
	}

	public void connectCandidateToGraphByInputs(GraphNode candidate, Map<String,GraphEdge> connections, Graph graph, Set<String> currentEndInputs, WSCInitializer init) {
		graph.nodeMap.put(candidate.getName(), candidate);
		graph.edgeList.addAll(connections.values());
		candidate.getIncomingEdgeList().addAll(connections.values());

		for (GraphEdge e : connections.values()) {
			GraphNode fromNode = graph.nodeMap.get(e.getFromNode().getName());
			fromNode.getOutgoingEdgeList().add(e);
		}
		for (String o : candidate.getOutputs()) {
			currentEndInputs.addAll(init.taxonomyMap.get(o).endNodeInputs);
		}
	}

	public void addToCandidateList(Service n, Set<Service> seenNode, Set<Service> relevant, List<Service> candidateList, WSCInitializer init) {
		seenNode.add(n);
		List<TaxonomyNode> taxonomyOutputs;
		if (n.getName().equals("start")) {
			taxonomyOutputs = new ArrayList<TaxonomyNode>();
			for (String outputVal : n.getOutputs()) {
				taxonomyOutputs.add(init.taxonomyMap.get(outputVal));
			}
		}
		else
			taxonomyOutputs = init.serviceMap.get(n.getName()).getTaxonomyOutputs();

		for (TaxonomyNode t : taxonomyOutputs) {
			// Add servicesWithInput from taxonomy node as potential candidates to be connected
			for (Service current : t.servicesWithInput.keySet()) {
				if (!seenNode.contains(current) && relevant.contains(current)) {
					candidateList.add(current);
					seenNode.add(current);
				}
			}
		}
	}

	public GPNode createNewTree(EvolutionState state, Set<String> inputSet, Set<String> outputSet) {
		WSCInitializer init = (WSCInitializer) state.initializer;

		// Find nodes that satisfy the given output
		Set<Service> services = new HashSet<Service>();
		Set<String> outputsToSatisfy = new HashSet<String>(outputSet);

		for (String o : outputSet) {
			if (outputsToSatisfy.contains(o)) {
				List<Service> candidates = init.taxonomyMap.get(o).servicesWithOutput;
				Collections.shuffle(candidates, init.random);
				Service chosen = null;
				candLoop:
				for (Service cand : candidates) {
					if (init.relevant.contains(cand)) {
						services.add(cand);
						chosen = cand;
						break candLoop;
					}
				}
				outputsToSatisfy.remove(o);

				// Check if other outputs can also be fulfilled by the chosen candidate, and remove them also
				Set<String> subsumed = init.getInputsSubsumed(outputsToSatisfy, chosen.outputs);
				outputsToSatisfy.removeAll(subsumed);
			}
		}

		GPNode root = recCreateNewTree(init, services, inputSet, outputSet);
		return root;
	}

	public GPNode recCreateNewTree(WSCInitializer init, Set<Service> services, Set<String> inputSet, Set<String> outputSet) {

		GPNode root;
		List<Service> satisfiedByStart = new ArrayList<Service>();

		// Check which nodes can be fully satisfied by the inputs provided
		checkSatisfiedByInputs(init, services, inputSet, satisfiedByStart);

		// Add these inputs to the list of subtrees
		List<GPNode> subtrees = new ArrayList<GPNode>();
		for (Service satisfied : satisfiedByStart) {
			ServiceGPNode servNode = new ServiceGPNode();
			servNode.setService(satisfied);
			subtrees.add(servNode);
		}

		// If not all nodes can be satisfied by the inputs provided
		Map<Service, Set<Service>> predecessorMap = new HashMap<Service, Set<Service>>();

		// Find predecessors in previous layers for each node, checking if start satisfies them.
		for (Service s : services) {
			if (!satisfiedByStart.contains(s)) {
				Set<Service> predecessors = findPredecessors(init, inputSet, s);
				predecessorMap.put(s, predecessors);
			}
		}

		// For each individual node, create a subtree with a sequence node root, and the node as the right child.
		for (Entry<Service, Set<Service>> entry : predecessorMap.entrySet()) {
			SequenceGPNode seq = new SequenceGPNode();
			subtrees.add(seq);
			GPNode[] children = new GPNode[2];

			// The left-hand side contains the tree for the predecessor
			GPNode leftChild = recCreateNewTree(init, entry.getValue(), inputSet, outputSet);
			leftChild.parent = seq;
			children[0] = leftChild;

			// The right-hand side contains the node satisfied
			ServiceGPNode rightChild = new ServiceGPNode();
			rightChild.setService(entry.getKey());
			rightChild.parent = seq;
			children[1] = rightChild;

			seq.children = children;
		}

		// If more than one subtree is created, put all of them under a parallel node parent.
		if (subtrees.size() > 1) {
			ParallelGPNode parNode = new ParallelGPNode();
			parNode.children = new GPNode[subtrees.size()];
			for (int i = 0; i < parNode.children.length; i++) {
				parNode.children[i] = subtrees.get(i);
				parNode.children[i].parent = parNode;
			}
			root = parNode;
		}
		else if (subtrees.size() == 1){
			root = subtrees.get(0);
		}
		else {
			throw new RuntimeException("No subtrees were created when recursing!");
		}

		return root;
	}

	public int checkSatisfiedByInputs(WSCInitializer init, Set<Service> services, Set<String> inputs, List<Service> satisfiedByStart) {
		for (Service s : services) {
			if (init.isSubsumed(s.getInputs(), inputs))
				satisfiedByStart.add(s);
		}
		return satisfiedByStart.size();
	}

	public Set<Service> findPredecessors(WSCInitializer init, Set<String> inputs, Service s) {
		Set<Service> predecessors = new HashSet<Service>();

		// Get only inputs that are not subsumed by the given composition inputs
		Set<String> inputsNotSatisfied = init.getInputsNotSubsumed(s.getInputs(), inputs);
		Set<String> inputsToSatisfy = new HashSet<String>(inputsNotSatisfied);

		// Find services to satisfy all inputs
		for (String i : inputsNotSatisfied) {
			if (inputsToSatisfy.contains(i)) {
				List<Service> candidates = init.taxonomyMap.get(i).servicesWithOutput;
				Collections.shuffle(candidates, init.random);

				Service chosen = null;
				candLoop:
				for(Service cand : candidates) {
					if (init.relevant.contains(cand) && cand.layer < s.layer) {
						predecessors.add(cand);
						chosen = cand;
						break candLoop;
					}
				}

				inputsToSatisfy.remove(i);

				// Check if other outputs can also be fulfilled by the chosen candidate, and remove them also
				Set<String> subsumed = init.getInputsSubsumed(inputsToSatisfy, chosen.outputs);
				inputsToSatisfy.removeAll(subsumed);
			}
		}
		return predecessors;
	}

}