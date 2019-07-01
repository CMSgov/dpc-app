package gov.cms.dpc.common.utils;

import com.codahale.metrics.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility class used to register a metric only once
 */
public class MetricFactory {
    private Class<?> klass;
    private MetricRegistry metricRegistry;

    /**
     * Construct for specific class and metric registry
     *
     * @param metricRegistry is expected to be a singleton
     * @param klass to use in names of metrics
     */
    public MetricFactory(MetricRegistry metricRegistry, Class<?> klass) {
        this.metricRegistry = metricRegistry;
        this.klass = klass;
    }

    /**
     * Register a timer
     *
     * @param name for the timer
     * @return the timer under the passed in name
     */
    public synchronized Timer registerTimer(String name) {
        final var metricName = MetricRegistry.name(klass, name);
        final var timers = metricRegistry.getTimers(MetricFilter.startsWith(metricName));
        return timers.containsKey(metricName) ? timers.get(metricName) : metricRegistry.timer(metricName);
    }

    /**
     * Register a meter
     *
     * @param name is unique
     * @return the meter created under the passed in name
     */
    public synchronized Meter registerMeter(String name) {
        final var metricName = MetricRegistry.name(klass, name);
        final var meters = metricRegistry.getMeters(MetricFilter.startsWith(metricName));
        return meters.containsKey(metricName) ? meters.get(metricName) : metricRegistry.meter(metricName);
    }

    /**
     * Register a cached gauge. A cached guage uses cached values to limit the number of polls of the supplier.
     *
     * @param name is unit
     * @param loadSupplier the supplier of the value of the gauge
     * @param <T> is the unit of the guage
     */
    public synchronized <T> void registerCachedGuage(String name, Supplier<T> loadSupplier) {
        final var metricName = MetricRegistry.name(klass, name);
        final var gauges = metricRegistry.getGauges(MetricFilter.startsWith(metricName));
        if (gauges.containsKey(metricName)) {
            return;
        }
        final var gauge = new CachedGaugeFromSupplier<>(1, TimeUnit.SECONDS, loadSupplier);
        metricRegistry.register(metricName, gauge);
    }

    /**
     * A gauge created from a supplier
     * @param <T> unit of the gauge
     */
    private static class CachedGaugeFromSupplier<T> extends CachedGauge<T> {
        private Supplier<T> loadSupplier;

        CachedGaugeFromSupplier(long timeout, TimeUnit timeUnit, Supplier<T> loadSupplier) {
            super(timeout, timeUnit);
            this.loadSupplier = loadSupplier;
        }

        @Override
        protected T loadValue() {
            return loadSupplier.get();
        }
    }
}
