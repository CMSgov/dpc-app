package gov.cms.dpc.newbe.client;

import gov.cms.dpc.newbe.models.NewbeExportRequest;
import gov.cms.dpc.newbe.models.NewbeExportResponse;
import gov.cms.dpc.newbe.models.NewbeJobStatus;

import java.util.UUID;

public interface NewbeClient {

    /**
     *  Check CapabilitiesStatement is "active"
     *  <a href="https://bcda.cms.gov/api-documentation/access-claims-data.html#check-api-status-2">...</a>
     */
    boolean healthy();

    NewbeExportResponse export(NewbeExportRequest request);

    NewbeJobStatus jobStatus(UUID jobId);
}
