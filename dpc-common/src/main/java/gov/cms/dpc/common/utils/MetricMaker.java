package gov.cms.dpc.common.utils;

import com.codahale.metrics.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility class used to register a metric only once
 */
public class MetricMaker {
    private Class<?> klass;
    private MetricRegistry metricRegistry;

    /**
     * Construct for specific class and metric registry
     *
     * @param metricRegistry is expected to be a singleton
     * @param klass to use in names of metrics
     */
    public MetricMaker(MetricRegistry metricRegistry, Class<?> klass) {
        this.metricRegistry = metricRegistry;
        this.klass = klass;
    }

    /**
     * Register a timer
     *
     * @param name for the timer
     * @return the timer under the passed in name
     */
    public Timer registerTimer(String name) {
        return registerMetric(name, Timer::new);
    }

    /**
     * Register a list of timers.
     *
     * @param names is a list of timer names
     * @return a map of timers associated with the passed in names
     */
    public Map<String, Timer> registerTimers(List<String> names) {
        final var map = new HashMap<String, Timer>();
        for (String name: names) {
            map.put(name, registerTimer(name + "Timer"));
        }
        return map;
    }

    /**
     * Register a meter
     *
     * @param name is unique
     * @return the meter created under the passed in name
     */
    public Meter registerMeter(String name) {
        return registerMetric(name, Meter::new);
    }

    /**
     * Register a list of meters.
     *
     * @param names is a list of meter names
     * @return a map of meters associated with the passed in names
     */
    public Map<String, Meter> registerMeters(List<String> names) {
        final var map = new HashMap<String, Meter>();
        for (String name: names) {
            map.put(name, registerMeter(name + "Meter"));
        }
        return map;
    }


    /**
     * Register a cached gauge. A cached gauge uses cached values to limit the number of polls of the supplier.
     *
     * @param name is unit
     * @param loadSupplier the supplier of the value of the gauge
     * @param <T> is the unit of the gauge
     */
    public <T> void registerCachedGauge(String name, Supplier<T> loadSupplier) {
        registerMetric(name, () -> new CachedGaugeFromSupplier<>(1, TimeUnit.SECONDS, loadSupplier));
    }

    /**
     * Register a metric or retrieve a previously registered metric
     *
     * @param name of the metric
     * @param supplier of the new metric if needed
     * @param <T> The type of metric
     * @return the register metric
     */
    @SuppressWarnings({"unchecked"})
    private synchronized <T extends Metric> T registerMetric(String name, MetricRegistry.MetricSupplier<T> supplier) {
        final var metricName = MetricRegistry.name(klass, name);
        final var metrics = metricRegistry.getMetrics();
        if (metrics.containsKey(metricName)) {
            return (T)metrics.get(metricName);
        } else {
            return metricRegistry.register(metricName, supplier.newMetric());
        }
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
