package gov.cms.dpc.newbe.client;

import gov.cms.dpc.newbe.models.NewbeExportRequest;
import gov.cms.dpc.newbe.models.NewbeExportResponse;
import gov.cms.dpc.newbe.models.NewbeJobStatus;

import java.util.UUID;

public interface NewbeClient {

    NewbeExportResponse export(NewbeExportRequest request);

    NewbeJobStatus jobStatus(UUID jobId);
}
