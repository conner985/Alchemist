package it.unibo.alchemist.test;

import static org.junit.Assert.fail;

import java.util.ConcurrentModificationException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unibo.alchemist.model.implementations.environments.Continuous2DEnvironment;
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule;
import it.unibo.alchemist.model.implementations.nodes.GenericNode;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Molecule;

/**
 *
 */
public class TestGenericNode {

    private static final int THREADS = 1000;

    /**
     * 
     */
    @Test
    @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON")
    public void testConcurrentAccess() {
        final Environment<Object> env = new Continuous2DEnvironment<>();
        @SuppressWarnings("serial")
        final GenericNode<Object> node = new GenericNode<Object>(env) {
            @Override
            protected Object createT() {
                return 0;
            }
        };
        final CountDownLatch cd = new CountDownLatch(THREADS);
        final Queue<Exception> queue = new ConcurrentLinkedQueue<>();
        final ExecutorService ex = Executors.newCachedThreadPool();
        IntStream.range(0, THREADS).forEach(i -> {
            ex.submit(() -> {
                try {
                    final Molecule m = new SimpleMolecule(Integer.toString(i));
                    cd.countDown();
                    try {
                        cd.await();
                    } catch (Exception e) {
                        queue.add(e);
                    }
                    node.setConcentration(m, i);
                    node.getContents().forEach((a, b) -> {
                        try {
                            Thread.sleep(1);
                        } catch (Exception e) {
                            queue.add(e);
                        }
                    });
                } catch (ConcurrentModificationException e) {
                    queue.add(e);
                }
            });
        });
        ex.shutdown();
        try {
            ex.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }
        if (!queue.isEmpty()) {
            fail(queue.size() + " concurrent errors detected.");
        }
    }

}
