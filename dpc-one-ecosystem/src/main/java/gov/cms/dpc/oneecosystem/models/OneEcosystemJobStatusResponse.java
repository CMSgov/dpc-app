package gov.cms.dpc.oneecosystem.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// pretty much copy/paste from
// https://bcda.cms.gov/api-documentation/access-claims-data.html#response-example-completed-job
public record OneEcosystemJobStatusResponse(
    Instant transactionTime,
    boolean requiresAccessToken,
    List<OneEcosystemJobStatusOutput> output,
    List<String> error,
    UUID JobID
) {
}
