package gov.cms.dpc.oneecosystem.models;

import java.time.Instant;
import java.util.List;

public record OneEcosystemJobStatusResponse(
    Instant transactionTime,
    boolean requiresAccessToken,
    List<OneEcosystemJobStatusOutput> output,
    List<String> error,
    String JobID
) {
}
