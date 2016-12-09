package wsc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.simple.SimpleFitness;
import ec.util.Parameter;

public class WSCIndividual extends GPIndividual {

	private static final long serialVersionUID = 1L;

	public WSCIndividual(){
		super();
		super.fitness = new SimpleFitness();
		super.species = new WSCSpecies();
	}

	public WSCIndividual(GPNode root) {
		super();
		super.fitness = new SimpleFitness();
		super.species = new WSCSpecies();
		super.trees = new GPTree[1];
		GPTree t = new GPTree();
		super.trees[0] = t;
		t.child = root;
	}

	@Override
	public Parameter defaultBase() {
		return new Parameter("wscindividual");
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof WSCIndividual) {
			return toString().equals(other.toString());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("digraph tree { ");
		builder.append(trees[0].child.toString());
		builder.append("}");
		return builder.toString();
	}

	@Override
	public WSCIndividual clone() {
		WSCIndividual wsci = new WSCIndividual((GPNode) super.trees[0].child.clone());
		wsci.fitness = (SimpleFitness) fitness.clone();
		wsci.species = species;
		return wsci;
	}

    public List< GPNode > getAllTreeNodes() {
        List< GPNode > allNodes = new ArrayList< GPNode >();
        Queue< GPNode > queue = new LinkedList< GPNode >();

        queue.offer( trees[ 0 ].child );

        while ( !queue.isEmpty() ) {
            GPNode current = queue.poll();
            allNodes.add( current );
            if ( current.children != null ) {
                for ( GPNode child : current.children )
                    allNodes.add( child );
            }
        }

        return allNodes;
    }

    public void replaceNode(GPNode node, GPNode replacement) {
        // Perform replacement if neither node is null
        if (node != null && replacement != null) {
            replacement = (GPNode) replacement.clone();
            GPNode parentNode = (GPNode) node.parent;
            if (parentNode == null) {
                super.trees[0].child = replacement;
            }
            else {
                replacement.parent = node.parent;
                for (int i = 0; i < parentNode.children.length; i++) {
                    if (parentNode.children[i] == node) {
                        parentNode.children[i] = replacement;
                        break;
                    }
                }
            }
        }
    }
}
