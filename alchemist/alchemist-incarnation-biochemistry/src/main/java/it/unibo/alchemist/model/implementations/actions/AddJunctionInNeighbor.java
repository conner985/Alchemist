/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.actions;

import it.unibo.alchemist.model.interfaces.Context;

import org.apache.commons.math3.random.RandomGenerator;

import it.unibo.alchemist.model.implementations.molecules.Junction;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.CellNode;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Reaction;

/**
 * Represent the action of add a junction between a neighbor and the current node. <br/>
 * This action only create the junction reference inside the neighbor, the current node totally ignore 
 * that a junction has been created.  <br/>
 * This is a part of the junction creation process. <br/>
 * See {@link AddJunctionInCell} for the other part of the process
 */
public class AddJunctionInNeighbor extends AbstractNeighborAction<Double> {

    private static final long serialVersionUID = 8670229402770243539L;

    private final Junction jun;
    private final Environment<Double> env;
    /**
     * 
     * @param junction the junction
     * @param n the current node which contains this action. It is NOT the node where the junction will be created.
     * @param e the environment
     * @param rg the random generator
     */
    public AddJunctionInNeighbor(final Environment<Double> e, final Node<Double> n, final Junction junction, final RandomGenerator rg) {
        super(n, e, rg);
        if (n instanceof CellNode) {
            addModifiedMolecule(junction);
            jun = junction; 
            env = e;
        } else {
            throw new UnsupportedOperationException("This Action can be set only in CellNodes");
        }
    }

    @Override
    public AddJunctionInNeighbor cloneAction(final Node<Double> n, final Reaction<Double> r) {
        return new AddJunctionInNeighbor(env, n, jun, getRandomGenerator());
    }

    /**
     * If no target node is given DO NOTHING. The junction can not be created.
     * @throws UnsupportedOperationException if this method is called.
     */
    @Override
    public void execute() {
        throw new UnsupportedOperationException("A junction CAN NOT be created without a target node.");
    }

    @Override
    public Context getContext() {
        return Context.NEIGHBORHOOD; // TODO try with local
    }

    /**
     * Create the junction that links the target node and the node when this action is executed. 
     */
    @Override
    public void execute(final Node<Double> targetNode) {
        if (targetNode instanceof CellNode) {
            ((CellNode) targetNode).addJunction(jun, getNode());
        } else {
            throw new UnsupportedOperationException("Can't add Junction in a node that it's not a CellNode");
        }
    }

    @Override 
    public String toString() {
        return "add junction " + jun.toString() + " in neighbor";
    }

    @Override
    public CellNode getNode() {
        return (CellNode) super.getNode();
    }

}
