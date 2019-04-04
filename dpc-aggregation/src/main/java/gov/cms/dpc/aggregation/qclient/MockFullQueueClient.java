package gov.cms.dpc.aggregation.qclient;


import gov.cms.dpc.common.models.JobModel;
import gov.cms.dpc.queue.Pair;

import java.util.*;

public class MockFullQueueClient extends AbstractMockQueueClient {
    private final List<String> testBeneficiaryIds = Arrays.asList("20140000008325", "20140000008326");

    public <T> Optional<Pair<UUID, T>> workJob() {
        final UUID uuid = UUID.randomUUID();
        return Optional.of(new Pair<>(
                uuid,
                (T) new JobModel(uuid, JobModel.ResourceType.PATIENT, "testProviderId", testBeneficiaryIds)
        ));
    }

}
