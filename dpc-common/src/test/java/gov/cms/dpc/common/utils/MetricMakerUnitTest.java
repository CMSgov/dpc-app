package gov.cms.dpc.common.utils;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MetricMakerUnitTest {
    private static final MetricRegistry registry = mock(MetricRegistry.class);
    private static final Class<?> klass = MetricMakerUnitTest.class;
    private static MetricMaker metricMaker;

    private static final List<String> metricNames = List.of("Foo", "Bar", "Baz");

    @BeforeAll
    static void setUp() {
        metricMaker = new MetricMaker(registry, klass);
    }

    @Test
    void testRegisterTimers() {
        Map<String, Timer> timerMap = metricMaker.registerTimers(metricNames);
        timerMap.forEach((name, timer) -> {
            assertTrue(metricNames.contains(name));
            assertEquals(timer, metricMaker.registerTimer(name));
        });
    }

    @Test
    void testRegisterMeters() {
        Map<String, Meter> meterMap = metricMaker.registerMeters(metricNames);
        meterMap.forEach((name, meter) -> {
            assertTrue(metricNames.contains(name));
            assertEquals(meter, metricMaker.registerMeter(name));
        });
    }
}
