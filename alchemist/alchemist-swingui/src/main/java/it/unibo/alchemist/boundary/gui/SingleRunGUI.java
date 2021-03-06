package it.unibo.alchemist.boundary.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unibo.alchemist.boundary.gui.effects.EffectSerializationFactory;
import it.unibo.alchemist.boundary.gui.effects.JEffectsTab;
import it.unibo.alchemist.boundary.gui.util.GraphicalMonitorFactory;
import it.unibo.alchemist.boundary.interfaces.GraphicalOutputMonitor;
import it.unibo.alchemist.boundary.l10n.LocalizedResourceBundle;
import it.unibo.alchemist.boundary.monitors.TimeStepMonitor;
import it.unibo.alchemist.core.interfaces.Simulation;

/**
 * Utility class for quickly creating non-reusable graphical interfaces.
 */
public final class SingleRunGUI {

    private static final Logger L = LoggerFactory.getLogger(SingleRunGUI.class);
    private static final float SCALE_FACTOR = 0.8f;
    private static final int FALLBACK_X_SIZE = 800;
    private static final int FALLBACK_Y_SIZE = 600;

    private SingleRunGUI() {
    }

    /**
     * Builds a single-use graphical interface.
     * 
     * @param sim
     *            the simulation for this GUI
     * @param <T>
     *            concentration type
     */
    public static <T> void make(final Simulation<T> sim) {
        make(sim, (File) null, JFrame.EXIT_ON_CLOSE);
    }

    /**
     * 
     * @param sim
     *            the simulation for this GUI
     * @param closeOperation
     *            the type of close operation for this GUI
     * @param <T>
     *            concentration type
     */
    public static <T> void make(final Simulation<T> sim, final int closeOperation) {
        make(sim, (File) null, closeOperation);
    }

    /**
     * Builds a single-use graphical interface.
     * 
     * @param sim
     *            the simulation for this GUI
     * @param effectsFile
     *            the effects file
     * @param <T>
     *            concentration type
     * @throws FileNotFoundException
     */
    public static <T> void make(final Simulation<T> sim, final String effectsFile) {
        make(sim, new File(effectsFile), JFrame.EXIT_ON_CLOSE);
    }

    /**
     * Builds a single-use graphical interface.
     * 
     * @param sim
     *            the simulation for this GUI
     * @param effectsFile
     *            the effects file
     * @param closeOperation
     *            the type of close operation for this GUI
     * @param <T>
     *            concentration type
     */
    public static <T> void make(final Simulation<T> sim, final String effectsFile, final int closeOperation) {
        make(sim, new File(effectsFile), closeOperation);
    }

    private static void errorLoadingEffects(final Throwable e) {
        L.error(LocalizedResourceBundle.getString("cannot_load_effects"), e);
    }

    /**
     * Builds a single-use graphical interface.
     * 
     * @param sim
     *            the simulation for this GUI
     * @param effectsFile
     *            the effects file
     * @param closeOperation
     *            the type of close operation for this GUI
     * @param <T>
     *            concentration type
     */
    public static <T> void make(final Simulation<T> sim, final File effectsFile, final int closeOperation) {
        final GraphicalOutputMonitor<T> main = GraphicalMonitorFactory.createMonitor(sim,
                e -> L.error("Cannot init the UI.", e));
        if (main instanceof Component) {
            final JFrame frame = new JFrame("Alchemist Simulator");
            // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setDefaultCloseOperation(closeOperation);
            final JPanel canvas = new JPanel();
            frame.getContentPane().add(canvas);
            canvas.setLayout(new BorderLayout());
            canvas.add((Component) main, BorderLayout.CENTER);
            /*
             * Upper area
             */
            final JPanel upper = new JPanel();
            upper.setLayout(new BoxLayout(upper, BoxLayout.X_AXIS));
            canvas.add(upper, BorderLayout.NORTH);
            final JEffectsTab<T> effects = new JEffectsTab<>(main, false);
            if (effectsFile != null) {
                try {
                    effects.setEffects(EffectSerializationFactory.effectsFromFile(effectsFile));
                } catch (IOException | ClassNotFoundException ex) {
                    errorLoadingEffects(ex);
                }
            }
            upper.add(effects);
            final TimeStepMonitor<T> time = new TimeStepMonitor<>();
            sim.addOutputMonitor(time);
            upper.add(time);
            /*
             * Go on screen
             */
            // frame.pack();
            final Optional<Dimension> size = Arrays
                    .stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
                    .map(GraphicsDevice::getDisplayMode).map(dm -> new Dimension(dm.getWidth(), dm.getHeight()))
                    .min((d1, d2) -> Double.compare(area(d1), area(d2)));
            size.ifPresent(d -> d.setSize(d.getWidth() * SCALE_FACTOR, d.getHeight() * SCALE_FACTOR));
            frame.setSize(size.orElse(new Dimension(FALLBACK_X_SIZE, FALLBACK_Y_SIZE)));
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
            /*
             * OutputMonitor's add to the sim must be done as the last operation
             */
            sim.addOutputMonitor(main);
        } else {
            L.error("The default monitor of {} is not compatible with Java Swing.", sim);
        }
    }

    private static double area(final Dimension d) {
        return d.getWidth() * d.getHeight();
    }

}
