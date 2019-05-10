package gov.cms.dpc.aggregation.qclient;

import gov.cms.dpc.queue.Pair;
import gov.cms.dpc.queue.models.JobModel;

import java.util.Optional;
import java.util.UUID;

public class MockEmptyQueueClient extends AbstractMockQueueClient {

    @Override
    public Optional<Pair<UUID, JobModel>> workJob() {
        return Optional.empty();
    }

    @Override
    public String queueType() {
        return "MockEmpty Queue";
    }
}
