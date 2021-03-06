/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.environments;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.danilopianini.util.SpatialIndex;

import com.google.common.collect.Sets;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import it.unibo.alchemist.core.interfaces.Simulation;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Layer;
import it.unibo.alchemist.model.interfaces.LinkingRule;
import it.unibo.alchemist.model.interfaces.Molecule;
import it.unibo.alchemist.model.interfaces.Neighborhood;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Position;

/**
 * Very generic and basic implementation for an environment. Basically, only
 * manages an internal set of nodes and their position.
 * 
 * @param <T>
 */
public abstract class AbstractEnvironment<T> implements Environment<T> {

    private static final long serialVersionUID = 0L;
    /**
     * The default monitor that will be loaded. If null, the GUI must default to
     * a compatible monitor.
     */
    protected static final String DEFAULT_MONITOR = null;
    private final TIntObjectHashMap<Position> nodeToPos = new TIntObjectHashMap<>();
    private final TIntObjectHashMap<Node<T>> nodes = new TIntObjectHashMap<Node<T>>();
    private final SpatialIndex<Node<T>> spatialIndex;
    private final Map<Molecule, Layer<T>> layers = new LinkedHashMap<>();
    private final TIntObjectHashMap<Neighborhood<T>> neighCache = new TIntObjectHashMap<>();
    private LinkingRule<T> rule;

    private Simulation<T> simulation;

    /**
     * @param internalIndex
     *            the {@link SpatialIndex} to use in order to efficiently
     *            retrieve nodes.
     */
    protected AbstractEnvironment(final SpatialIndex<Node<T>> internalIndex) {
        spatialIndex = Objects.requireNonNull(internalIndex);
    }

    @Override
    public void addLayer(final Molecule m, final Layer<T> l) {
        if (layers.put(m, l) != null) {
            throw new IllegalStateException("Two layers have been associated to " + m);
        }
    }

    @Override
    public final void addNode(final Node<T> node, final Position p) {
        if (nodeShouldBeAdded(node, p)) {
            final Position actualPosition = computeActualInsertionPosition(node, p);
            setPosition(node, actualPosition);
            nodes.put(node.getId(), node);
            spatialIndex.insert(node, actualPosition.getCartesianCoordinates());
            /*
             * Neighborhood computation
             */
            updateNeighborhood(node);
            /*
             * Reaction and dependencies creation on the engine. This must be
             * executed only when the neighborhoods have been correctly computed,
             * and only if a simulation engine have actually been attached.
             */
            ifEngineAvailable(s -> s.nodeAdded(node));
            /*
             * Call the subclass method.
             */
            nodeAdded(node, p, getNeighborhood(node));
        }
    }

    /**
     * Allows subclasses to tune the actual position of a node, applying spatial
     * constrains at node addition.
     * 
     * @param node
     *            the node
     * @param p
     *            the original (requested) position
     * @return the actual position where the node should be located
     */
    protected abstract Position computeActualInsertionPosition(Node<T> node, Position p);

    @Override
    public void forEach(final Consumer<? super Node<T>> action) {
        getNodes().forEach(action);
    }

    private Stream<Operation> foundNeighbors(final Node<T> center, final Neighborhood<T> oldNeighborhood, final Neighborhood<T> newNeighborhood) {
        return newNeighborhood.getNeighbors().stream()
                .filter(neigh -> oldNeighborhood == null || !oldNeighborhood.contains(neigh))
                .filter(neigh -> !getNeighborhood(neigh).contains(center))
                .map(n -> new Operation(center, n, true));
    }

    private Set<Node<T>> getAllNodesInRange(final Position center, final double range) {
        final List<Position> boundingBox = center.buildBoundingBox(range);
        assert boundingBox.size() == getDimensions();
        final double[][] queryArea = new double[getDimensions()][];
        for (int i = 0; i < queryArea.length; i++) {
            queryArea[i] = boundingBox.get(i).getCartesianCoordinates();
        }
        final Collection<Node<T>> result = spatialIndex.query(queryArea);
        final Iterator<Node<T>> it = result.iterator();
        while (it.hasNext()) {
            if (getPosition(it.next()).getDistanceTo(center) > range) {
                it.remove();
            }
        }
        return new LinkedHashSet<>(result);
    }

    @Override
    public double getDistanceBetweenNodes(final Node<T> n1, final Node<T> n2) {
        final Position p1 = getPosition(n1);
        final Position p2 = getPosition(n2);
        return p1.getDistanceTo(p2);
    }

    @Override
    public Optional<Layer<T>> getLayer(final Molecule m) {
        return Optional.ofNullable(layers.get(m));
    }

    @Override
    public Set<Layer<T>> getLayers() {
        return Sets.newLinkedHashSet(layers.values());
    }

    @Override
    public final LinkingRule<T> getLinkingRule() {
        return rule;
    }


    @Override
    public final Neighborhood<T> getNeighborhood(@Nonnull final Node<T> center) {
        return neighCache.get(Objects.requireNonNull(center).getId());
    }

    /**
     * @return a pointer to the neighborhoods cache structure
     */
    protected final TIntObjectHashMap<Neighborhood<T>> getNeighborsCache() {
        return neighCache;
    }

    @Override
    public Node<T> getNodeByID(final int id) {
        return nodes.get(id);
    }

    @Override
    public Collection<Node<T>> getNodes() {
        return Collections.unmodifiableCollection(nodes.valueCollection());
    }

    @Override
    public int getNodesNumber() {
        return nodes.size();
    }

    @Override
    public Set<Node<T>> getNodesWithinRange(final Node<T> center, final double range) {
        /*
         * Remove the center node
         */
        final Set<Node<T>> res = getAllNodesInRange(getPosition(center), range);
        res.remove(center);
        return res;
    }

    @Override
    public Set<Node<T>> getNodesWithinRange(final Position center, final double range) {
        /*
         * Collect every node in range
         */
        return getAllNodesInRange(center, range);
    }

    @Override
    public final Position getPosition(final Node<T> node) {
        return nodeToPos.get(node.getId());
    }

    @Override
    public String getPreferredMonitor() {
        return DEFAULT_MONITOR;
    }

    @Override
    public Simulation<T> getSimulation() {
        return simulation;
    }

    @Override
    public double[] getSizeInDistanceUnits() {
        return getSize();
    }

    private void ifEngineAvailable(final Consumer<Simulation<T>> r) {
        Optional.ofNullable(getSimulation()).ifPresent(r);
    }

    @Override
    public Iterator<Node<T>> iterator() {
        return getNodes().iterator();
    }

    private Stream<Operation> lostNeighbors(final Node<T> center, final Neighborhood<T> oldNeighborhood, final Neighborhood<T> newNeighborhood) {
        return Optional.ofNullable(oldNeighborhood)
        .map(Neighborhood::getNeighbors)
        .orElse(Collections.emptyList())
        .stream()
        .filter(neigh -> !newNeighborhood.contains(neigh))
        .filter(neigh -> getNeighborhood(neigh).contains(center))
        .map(n -> new Operation(center, n, false));
    }

    /**
     * This method gets called once a node has been added, and its neighborhood has been computed and memorized.
     * 
     * @param node the node
     * @param position the position of the node
     * @param neighborhood the current neighborhood of the node
     */
    protected abstract void nodeAdded(Node<T> node, Position position, Neighborhood<T> neighborhood);

    /**
     * This method gets called once a node has been removed.
     * 
     * @param node
     *            the node
     * @param neighborhood
     *            the OLD neighborhood of the node (it is no longer in sync with
     *            the {@link Environment} status)
     */
    protected abstract void nodeRemoved(Node<T> node, Neighborhood<T> neighborhood);

    /**
     * Allows subclasses to determine wether or not a {@link Node} should
     * actually get added to this environment.
     * 
     * @param node
     *            the node
     * @param p
     *            the original (requested) position
     * @return true if the node should be added to this environment, false
     *         otherwise
     */
    protected abstract boolean nodeShouldBeAdded(Node<T> node, Position p);

    private Queue<Operation> recursiveOperation(final Node<T> origin) {
        final Neighborhood<T> newNeighborhood = rule.computeNeighborhood(Objects.requireNonNull(origin), this);
        final Neighborhood<T> oldNeighborhood = neighCache.put(origin.getId(), newNeighborhood);
        return toQueue(origin, oldNeighborhood, newNeighborhood);
    }

    private Queue<Operation> recursiveOperation(final Node<T> origin, final Node<T> destination, final boolean isAdd) {
        if (isAdd) {
            ifEngineAvailable(s -> s.neighborAdded(origin, destination));
        } else {
            ifEngineAvailable(s -> s.neighborRemoved(origin, destination));
        }
        final Neighborhood<T> newNeighborhood = rule.computeNeighborhood(Objects.requireNonNull(destination), this);
        final Neighborhood<T> oldNeighborhood = neighCache.put(destination.getId(), newNeighborhood);
        return toQueue(destination, oldNeighborhood, newNeighborhood);
    }

    @Override
    public final void removeNode(@Nonnull final Node<T> node) {
        nodes.remove(Objects.requireNonNull(node).getId());
        final Position pos = nodeToPos.remove(node.getId());
        spatialIndex.remove(node, pos.getCartesianCoordinates());
        /*
         * Neighborhood update
         */
        final Neighborhood<T> neigh = neighCache.remove(node.getId());
        for (final Node<T> n : neigh) {
            neighCache.get(n.getId()).removeNeighbor(node);
        }
        /*
         * Update all the reactions which may have been affected by the node
         * removal
         */
        ifEngineAvailable(s -> s.nodeRemoved(node, neigh));
        /*
         * Call subclass remover
         */
        nodeRemoved(node, neigh);
    }

    @Override
    public void setLinkingRule(final LinkingRule<T> r) {
        rule = Objects.requireNonNull(r);
    }

    /**
     * Adds or changes a position entry in the position map.
     * 
     * @param n
     *            the node
     * @param p
     *            its new position
     */
    protected final void setPosition(final Node<T> n, final Position p) {
        final Position pos = nodeToPos.put(n.getId(), p);
        if (pos != null && !spatialIndex.move(n, pos.getCartesianCoordinates(), p.getCartesianCoordinates())) {
            throw new IllegalArgumentException("Tried to move a node not previously present in the environment: \n"
                    + "Node: " + n + "\n" + "Requested position" + p);
        }
    }

    @Override
    public final void setSimulation(final Simulation<T> s) {
        if (simulation == null) {
            simulation = s;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public Spliterator<Node<T>> spliterator() {
        return getNodes().spliterator();
    }

    private Queue<Operation> toQueue(final Node<T> center, final Neighborhood<T> oldNeighborhood, final Neighborhood<T> newNeighborhood) {
        return Stream.concat(
                lostNeighbors(center, oldNeighborhood, newNeighborhood),
                foundNeighbors(center, oldNeighborhood, newNeighborhood))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * After a node movement, recomputes the neighborhood, also notifying the
     * running simulation about the modifications. This allows movement actions
     * to be defined as LOCAL (they should be normally considered GLOBAL).
     * 
     * @param node
     *            the node that has been moved
     */
    protected final void updateNeighborhood(final Node<T> node) {
        /*
         * The following optimization allows to define as local the context of
         * reactions which are actually including a move, which should be
         * normally considered global. This because for each node which is
         * detached, all the dependencies are updated, ensuring the soundness.
         */
        if (Objects.requireNonNull(rule).isLocallyConsistent()) {
            final Neighborhood<T> newNeighborhood = rule.computeNeighborhood(Objects.requireNonNull(node), this);
            final Neighborhood<T> oldNeighborhood = neighCache.put(node.getId(), newNeighborhood);
            if (oldNeighborhood != null) {
                final Iterator<Node<T>> iter = oldNeighborhood.iterator();
                while (iter.hasNext()) {
                    final Node<T> neighbor = iter.next();
                    if (!newNeighborhood.contains(neighbor)) {
                        /*
                         * Neighbor lost
                         */
                        iter.remove();
                        final Neighborhood<T> neighborsNeighborhood = neighCache.get(neighbor.getId());
                        neighborsNeighborhood.removeNeighbor(node);
                        ifEngineAvailable(s -> s.neighborRemoved(node, neighbor));
                    }
                }
            }
            for (final Node<T> n : newNeighborhood) {
                if (oldNeighborhood == null || !oldNeighborhood.contains(n)) {
                    /*
                     * If it's a new neighbor
                     */
                    neighCache.get(n.getId()).addNeighbor(node);
                    ifEngineAvailable(s -> s.neighborAdded(node, n));
                }
            }
        } else {
            final Queue<Operation> operations = recursiveOperation(node);
            final TIntSet processed = new TIntHashSet(getNodesNumber());
            processed.add(node.getId());
            while (!operations.isEmpty()) {
                final Operation next = operations.poll();
                final Node<T> dest = next.destination;
                final int destId = dest.getId();
                if (!processed.contains(destId)) {
                    operations.addAll(recursiveOperation(next.origin, next.destination, next.isAdd));
                    processed.add(destId);
                }
            }
        }
    }

    private class Operation {
        private final Node<T> origin;
        private final Node<T> destination;
        private final boolean isAdd;
        Operation(final Node<T> origin, final Node<T> destination, final boolean isAdd) {
            this.origin = origin;
            this.destination = destination;
            this.isAdd = isAdd;
        }
        @Override
        public String toString() {
            return origin + (isAdd ? " discovered " : " lost ") + destination;
        }
    }

}
