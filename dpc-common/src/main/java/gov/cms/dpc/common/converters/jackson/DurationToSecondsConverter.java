package gov.cms.dpc.common.converters.jackson;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.time.Duration;

/**
 * Serialize the provided {@link Duration} to a {@link Long} representing the duration in seconds.
 */
public class DurationToSecondsConverter extends StdConverter<Duration, Long> {

    private DurationToSecondsConverter() {
        // Not used
    }

    @Override
    public Long convert(Duration duration) {
        return duration.toSeconds();
    }
}
