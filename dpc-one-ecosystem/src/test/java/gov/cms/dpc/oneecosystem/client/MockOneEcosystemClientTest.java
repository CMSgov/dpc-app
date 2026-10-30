package gov.cms.dpc.oneecosystem.client;

import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockOneEcosystemClientTest {

    private static final List<String> ALL_RESOURCE_TYPES = List.of("Patient", "Coverage", "ExplanationOfBenefit");

    @TempDir
    Path exportPath;

    @Test
    void testInvalidJobReturnsBackendError() {
        MockOneEcosystemClient client = new MockOneEcosystemClient(exportPath);

        var invalidJob = client.jobStatus(UUID.randomUUID());
        assertEquals(400, invalidJob.statusCode());
    }

    @Test
    void testJobCompletesOnThirdPoll() {
        MockOneEcosystemClient client = new MockOneEcosystemClient(exportPath);

        OneEcosystemExportResponse response = client.export(request());
        final UUID jobId = getJobIdFromResponse(response);

        assertEquals(202, response.statusCode());
        assertEquals(202, client.jobStatus(jobId).statusCode());
        assertEquals(202, client.jobStatus(jobId).statusCode());

        var completedJob = client.jobStatus(jobId);
        assertEquals(200, completedJob.statusCode());
    }

    @Test
    void testCompletedJobHasThreeFileUrls() {
        MockOneEcosystemClient client = new MockOneEcosystemClient(exportPath);

        OneEcosystemExportResponse response = client.export(request());
        final UUID jobId = getJobIdFromResponse(response);
        client.jobStatus(jobId);
        client.jobStatus(jobId);

        var completedJob = client.jobStatus(jobId);
        assertEquals(200, completedJob.statusCode());

        assertEquals(ALL_RESOURCE_TYPES.size(), completedJob.body().output().size());
        for (int index = 0; index < ALL_RESOURCE_TYPES.size(); index++) {
            String resourceType = ALL_RESOURCE_TYPES.get(index);
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
        MockOneEcosystemClient client = new MockOneEcosystemClient(exportPath);

        OneEcosystemExportResponse response = client.export(request());
        final UUID jobId = getJobIdFromResponse(response);
        client.jobStatus(jobId);
        client.jobStatus(jobId);

        var completedJob = client.jobStatus(jobId);
        assertEquals(200, completedJob.statusCode());

        // verify unzipped that mock files are created
        for (String resourceType : ALL_RESOURCE_TYPES) {
            Path output = Path.of(exportPath + "/" + jobId + "-" + resourceType + ".ndjson");
            assertTrue(Files.exists(output));
        }
        // verify gzipped
        for (String resourceType : ALL_RESOURCE_TYPES) {
            Path output = Path.of(exportPath + "/" + jobId + "-" + resourceType + ".ndjson.gz");
            assertTrue(Files.exists(output));
        }
    }

    @Test
    void testJobCreatesFilesOnlyForRequestedResourceTypes() {
        MockOneEcosystemClient client = new MockOneEcosystemClient(exportPath);

        OneEcosystemExportResponse response = client.export(request(List.of("Patient")));
        final UUID jobId = getJobIdFromResponse(response);
        client.jobStatus(jobId);
        client.jobStatus(jobId);

        var completedJob = client.jobStatus(jobId);
        assertEquals(List.of("Patient"), completedJob.body().output().stream()
            .map(output -> output.type())
            .toList());

        // verify patient files are created
        assertTrue(Files.exists(Path.of(exportPath + "/" + jobId + "-Patient.ndjson")));
        assertTrue(Files.exists(Path.of(exportPath + "/" + jobId + "-Patient.ndjson.gz")));

        // verify other files are not created
        assertFalse(Files.exists(Path.of(exportPath + "/" + jobId + "-Coverage.ndjson")));
        assertFalse(Files.exists(Path.of(exportPath + "/" + jobId + "-Coverage.ndjson.gz")));
        assertFalse(Files.exists(Path.of(exportPath + "/" + jobId + "-ExplanationOfBenefit.ndjson")));
        assertFalse(Files.exists(Path.of(exportPath + "/" + jobId + "-ExplanationOfBenefit.ndjson.gz")));
    }

    private UUID getJobIdFromResponse(OneEcosystemExportResponse response) {
        return UUID.fromString(response.contentLocation().substring(response.contentLocation().lastIndexOf('/') + 1));
    }

    private OneEcosystemExportRequest request() {
        return request(ALL_RESOURCE_TYPES);
    }

    private OneEcosystemExportRequest request(List<String> resourceTypes) {
        return new OneEcosystemExportRequest(
            List.of("ABC0123ABCD"),
            Instant.now(),
            resourceTypes,
            "ExplanationOfBenefit?outcome=complete" // this means other resourceTypes are not _typeFilter'd
        );
    }
}
