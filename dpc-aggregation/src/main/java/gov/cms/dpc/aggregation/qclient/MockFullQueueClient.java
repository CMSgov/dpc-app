package gov.cms.dpc.aggregation.qclient;

import gov.cms.dpc.queue.models.JobModel;
import gov.cms.dpc.queue.Pair;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.util.*;

public class MockFullQueueClient extends AbstractMockQueueClient {
    private final List<String> testBeneficiaryIds = Arrays.asList("20140000008325", "20140000008326");

    public Optional<Pair<UUID, JobModel>> workJob() {
        final UUID uuid = UUID.randomUUID();
        return Optional.of(new Pair<>(
                uuid,
                new JobModel(uuid, List.of(ResourceType.Patient), "testProviderId", testBeneficiaryIds)
        ));
    }

    @Override
    public String queueType() {
        return "MockFullQueue";
    }

}
