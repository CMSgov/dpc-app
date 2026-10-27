package gov.cms.dpc.newbe.models;

import java.util.UUID;
import java.util.List;

public record NewbeExportRequest(UUID groupId, List<String> resourceTypes) {
}
