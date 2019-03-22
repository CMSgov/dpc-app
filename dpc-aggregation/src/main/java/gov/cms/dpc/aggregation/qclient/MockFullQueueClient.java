package gov.cms.dpc.aggregation.qclient;


import gov.cms.dpc.common.models.JobModel;
import gov.cms.dpc.queue.Pair;

import java.util.*;

public class MockFullQueueClient extends AbstractMockQueueClient {
    private final List<String> testBeneficiaryIds = Arrays.asList("20140000008325", "20140000008326");

    public <T> Optional<Pair<UUID, T>> workJob() {
        return Optional.of(new Pair<>(
                UUID.randomUUID(),
                (T) new JobModel("testProviderId", testBeneficiaryIds)
        ));
    }

}
