package gov.cms.dpc.oneecosystem.client;

import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatus;

import java.util.UUID;

public interface OneEcosystemClient {

    /**
     *  Check CapabilitiesStatement is "active"
     *  <a href="https://bcda.cms.gov/api-documentation/access-claims-data.html#check-api-status-2">...</a>
     */
    boolean healthy();

    OneEcosystemExportResponse export(OneEcosystemExportRequest request);

    OneEcosystemJobStatus jobStatus(UUID jobId);
}
