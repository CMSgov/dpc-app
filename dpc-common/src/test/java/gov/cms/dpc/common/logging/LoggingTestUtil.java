package gov.cms.dpc.common.logging;

import gov.cms.dpc.common.logging.filters.LoggingConstants;

public class LoggingTestUtil {
    static Iterable<String> excludedUris() {
        return LoggingConstants.EXCLUDED_URIS;
    }
}
