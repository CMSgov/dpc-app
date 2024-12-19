package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.common.filter.FilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotEmpty;
import java.util.*;
import java.util.stream.Collectors;

@JsonTypeName("secret-filter-factory")
public class SecretLoggingFilter implements FilterFactory<ILoggingEvent> {
	private static final Logger logger = LoggerFactory.getLogger(SecretLoggingFilter.class);

	private final Map<String, String> envVars = System.getenv();

	@NotEmpty
	private List<String> secrets = new ArrayList<>();

	@JsonProperty("secrets") // Required for Jackson to build this correctly
	public void setSecrets(List<String> secrets) {
		this.secrets = secrets;
	}

	public SecretLoggingFilter() {
		Map<String, String> envVars = System.getenv();
	}

	@Override
	public Filter<ILoggingEvent> build() {
		// Clean the secrets list
        List<String> containsSecrets = secrets.stream().filter(envVars::containsKey).collect(Collectors.toList());

		return new Filter<>() {
			@Override
			public FilterReply decide(ILoggingEvent event) {
				for (String secretName : containsSecrets) {
					if (event.getFormattedMessage().contains(envVars.get(secretName))) {
                        logger.warn("Suppressing log, attempted to write {} in {}", secretName, event.getLoggerName());
						return FilterReply.DENY;
					}
				}
				return FilterReply.NEUTRAL;
			}
		};
	}
}
