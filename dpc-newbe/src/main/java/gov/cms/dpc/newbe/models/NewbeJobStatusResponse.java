package gov.cms.dpc.newbe.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// pretty much copy/paste from
// https://bcda.cms.gov/api-documentation/access-claims-data.html#response-example-completed-job
public record NewbeJobStatusResponse(
    Instant transactionTime,
    boolean requiresAccessToken,
    List<NewbeJobStatusOutput> output,
    List<String> error,
    UUID JobID
) {
}
