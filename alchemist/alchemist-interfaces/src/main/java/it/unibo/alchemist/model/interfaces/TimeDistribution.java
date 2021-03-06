/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.interfaces;

import java.io.Serializable;

/**
 * This interface represents a temporal distribution for any event.
 * 
 * @param <T> concentration type
 */
public interface TimeDistribution<T> extends Cloneable, Serializable {

    /**
     * Updates the internal status.
     * 
     * @param curTime current time
     * @param executed true if the reaction has just been executed
     * @param param a parameter passed by the reaction
     * @param env the current environment
     */
    void update(Time curTime, boolean executed, double param, Environment<T> env);

    /**
     * @return the next time at which the event will occur
     */
    Time getNextOccurence();

    /**
     * @return how many times per time unit the event will happen on average
     */
    double getRate();

    /**
     * @return an exact copy of this {@link TimeDistribution}
     */
    TimeDistribution<T> clone();

}
