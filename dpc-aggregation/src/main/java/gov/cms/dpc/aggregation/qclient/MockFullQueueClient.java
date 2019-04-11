package gov.cms.dpc.aggregation.qclient;


import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.Pair;

import java.util.*;

public class MockFullQueueClient extends AbstractMockQueueClient {
    private final List<String> testBeneficiaryIds = Arrays.asList("20140000008325", "20140000008326");

    public Optional<Pair<UUID, JobModel>> workJob() {
        final UUID uuid = UUID.randomUUID();
        return Optional.of(new Pair<>(
                uuid,
                new JobModel(uuid, JobModel.ResourceType.PATIENT, "testProviderId", testBeneficiaryIds)
        ));
    }

    @Override
    public String queueType() {
        return "MockFullQueue";
    }

}
