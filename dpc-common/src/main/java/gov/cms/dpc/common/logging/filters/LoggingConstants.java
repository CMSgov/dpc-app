package gov.cms.dpc.common.logging.filters;

import java.util.Set;

public class LoggingConstants {
    private LoggingConstants() {
    }

    static final Set<String> EXCLUDED_URIS = Set.of("/healthcheck", "/v1/version", "/api/v1/version", "/ping");
}
