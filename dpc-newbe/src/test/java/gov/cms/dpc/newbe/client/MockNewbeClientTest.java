package gov.cms.dpc.newbe.client;

import gov.cms.dpc.newbe.models.NewbeExportRequest;
import gov.cms.dpc.newbe.models.NewbeExportResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockNewbeClientTest {

    @TempDir
    Path exportPath;

    @Test
    void testJobCompletesOnThirdPoll() {
        MockNewbeClient client = new MockNewbeClient(exportPath);

        NewbeExportResponse response = client.export(request());
        final UUID jobId = getJobIdFromResponse(response);

        assertEquals(202, response.statusCode());
        assertEquals(202, client.jobStatus(jobId).statusCode());
        assertEquals(202, client.jobStatus(jobId).statusCode());

        var completedJob = client.jobStatus(jobId);
        assertEquals(200, completedJob.statusCode());
    }

    @Test
    void testCompletedJobHasThreeFileUrls() {
        MockNewbeClient client = new MockNewbeClient(exportPath);

        NewbeExportResponse response = client.export(request());
        final UUID jobId = getJobIdFromResponse(response);
        client.jobStatus(jobId);
        client.jobStatus(jobId);

        var completedJob = client.jobStatus(jobId);
        assertEquals(200, completedJob.statusCode());

        List<String> resourceTypes = List.of("Patient", "Coverage", "ExplanationOfBenefit");
        assertEquals(resourceTypes.size(), completedJob.body().output().size());
        for (int index = 0; index < resourceTypes.size(); index++) {
            String resourceType = resourceTypes.get(index);
            var output = completedJob.body().output().get(index);
            assertEquals(resourceType, output.type());
            assertEquals(
                    "/data/" + jobId + "/" + jobId + "-" + resourceType + ".ndjson",
                    output.url()
            );
        }
    }

    @Test
    void testJobCreatesSixFakeNdjsonFiles() {
        MockNewbeClient client = new MockNewbeClient(exportPath);

        NewbeExportResponse response = client.export(request());
        final UUID jobId = getJobIdFromResponse(response);
        client.jobStatus(jobId);
        client.jobStatus(jobId);

        var completedJob = client.jobStatus(jobId);
        assertEquals(200, completedJob.statusCode());

        // verify unzipped that mock files are created
        for (String resourceType : List.of("Patient", "Coverage", "ExplanationOfBenefit")) {
            Path output = Path.of(exportPath + "/" + jobId + "-" + resourceType + ".ndjson");
            assertTrue(Files.exists(output));
        }
        // verify gzipped
        for (String resourceType : List.of("Patient", "Coverage", "ExplanationOfBenefit")) {
            Path output = Path.of(exportPath + "/" + jobId + "-" + resourceType + ".ndjson.gz");
            assertTrue(Files.exists(output));
        }
    }

    private UUID getJobIdFromResponse(NewbeExportResponse response) {
        return UUID.fromString(response.contentLocation().substring(response.contentLocation().lastIndexOf('/') + 1));
    }

    private NewbeExportRequest request() {
        return new NewbeExportRequest(UUID.randomUUID());
    }
}
