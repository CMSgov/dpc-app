package gov.cms.dpc.aggregation.qclient;

import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.JobStatus;
import gov.cms.dpc.queue.Pair;

import java.util.Optional;
import java.util.UUID;

public class MockEmptyQueueClient extends AbstractMockQueueClient {

    public <T> Optional<Pair<UUID, T>> workJob() {
        return Optional.empty();
    }
}
