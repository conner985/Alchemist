/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.expressions.implementations;

import it.unibo.alchemist.expressions.interfaces.ITreeNode;
import org.danilopianini.lang.util.FasterString;

import java.util.Map;


/**
 */
public class ConstTreeNode extends ATreeNode<FasterString> {

    private static final long serialVersionUID = 8358898580537639569L;

    /**
     * @param data
     *            a FasterString representation of the constant
     */
    public ConstTreeNode(final FasterString data) {
        super(data, null, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see alice.alchemist.expressions.interfaces.ITreeNode#getType()
     */
    @Override
    public Type getType() {
        return Type.CONST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * alice.alchemist.expressions.implementations.ATreeNode#getValue(java.util
     * .Map)
     */
    @Override
    public FasterString getValue(final Map<FasterString, ITreeNode<?>> mp) {
        return getData();
    }

    /*
     * (non-Javadoc)
     * 
     * @see alice.alchemist.expressions.implementations.ATreeNode#toString()
     */
    @Override
    public String toString() {
        return getData().toString();
    }

}
