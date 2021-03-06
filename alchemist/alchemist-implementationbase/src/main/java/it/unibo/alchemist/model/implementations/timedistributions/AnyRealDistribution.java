package it.unibo.alchemist.model.implementations.timedistributions;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.implementations.utils.RealDistributionUtil;
import it.unibo.alchemist.model.interfaces.Condition;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Time;

/**
 * This class is able to use any distribution provided by Apache Math 3 as a
 * subclass of {@link RealDistribution}. Being generic, however, it does not
 * allow for dynamic rate tuning (namely, it can't be used to generate events
 * with varying frequency based on
 * {@link Condition#getPropensityConditioning()}.
 * 
 * @param <T>
 *            concentration type
 */
public class AnyRealDistribution<T> extends AbstractDistribution<T> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "All the implementations of RealDistribution also implement Serializable")
    private final RealDistribution distribution;

    /**
     * @param rng
     *            the {@link RandomGenerator}
     * @param distribution
     *            the distribution name (case insensitive). Must be mappable to an entity implementing {@link RealDistribution}
     * @param parameters
     *            the parameters for the distribution
     */
    public AnyRealDistribution(final RandomGenerator rng, final String distribution, final double... parameters) {
        this(DoubleTime.ZERO_TIME, rng, distribution, parameters);
    }

    /**
     * @param start
     *            the initial time
     * @param rng
     *            the {@link RandomGenerator}
     * @param distribution
     *            the distribution name (case insensitive). Must be mappable to an entity implementing {@link RealDistribution}
     * @param parameters
     *            the parameters for the distribution
     */
    public AnyRealDistribution(final Time start, final RandomGenerator rng, final String distribution, final double... parameters) {
        this(start, RealDistributionUtil.makeRealDistribution(rng, distribution, parameters));
    }

    /**
     * @param distribution
     *            the {@link AnyRealDistribution} to use. To ensure
     *            reproducibility, such distribution must get created using the
     *            simulation {@link RandomGenerator}.
     */
    public AnyRealDistribution(final RealDistribution distribution) {
        this(DoubleTime.ZERO_TIME, distribution);
    }

    /**
     * @param start
     *            distribution start time
     * @param distribution
     *            the {@link AnyRealDistribution} to use. To ensure
     *            reproducibility, such distribution must get created using the
     *            simulation {@link RandomGenerator}.
     */
    public AnyRealDistribution(final Time start, final RealDistribution distribution) {
        super(start);
        this.distribution = distribution;
    }

    @Override
    public double getRate() {
        return distribution.getNumericalMean();
    }

    @Override
    protected final void updateStatus(final Time curTime, final boolean executed, final double param, final Environment<T> env) {
        if (param != getRate()) {
            throw new IllegalStateException(getClass().getSimpleName() + " does not allow to dynamically tune the rate.");
        }
        setTau(new DoubleTime(curTime.toDouble() + distribution.sample()));
    }

    @Override
    @SuppressFBWarnings(value = "CN_IDIOM_NO_SUPER_CALL", justification = "Cloneable should not get used anyway")
    public AbstractDistribution<T> clone() {
        return new AnyRealDistribution<>(getNextOccurence(), distribution);
    }

}
