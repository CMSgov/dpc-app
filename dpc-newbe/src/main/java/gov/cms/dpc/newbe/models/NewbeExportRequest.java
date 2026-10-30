package gov.cms.dpc.newbe.models;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record NewbeExportRequest(
    List<String> mbis,
    Instant since,
    List<String> resourceTypes,
    String typeFilter
) {
    public NewbeExportRequest {
        mbis = List.copyOf(mbis);
        Objects.requireNonNull(since, "since param is required");
        resourceTypes = List.copyOf(resourceTypes);
        Objects.requireNonNull(typeFilter, "typeFilter param is required");
    }
}
