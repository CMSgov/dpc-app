package gov.cms.dpc.oneecosystem.client;

import gov.cms.dpc.oneecosystem.models.OneEcosystemExportRequest;
import gov.cms.dpc.oneecosystem.models.OneEcosystemExportResponse;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatus;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatusOutput;
import gov.cms.dpc.oneecosystem.models.OneEcosystemJobStatusResponse;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class MockOneEcosystemClient implements OneEcosystemClient {

    private static final int POLL_RETRIES_BEFORE_FILE_DOWNLOAD = 2;
    private static final String[] ALL_RESOURCE_TYPES = {"Patient", "Coverage", "ExplanationOfBenefit"};

    private final Path exportPath;
    private final Map<UUID, MockJob> jobs = new HashMap<>();

    public MockOneEcosystemClient(Path exportPath) {
        this.exportPath = exportPath;
    }

    @Override
    public boolean healthy() {
        return true;
    }

    /**
     * response still tbd, but BCDA docs say to use content-location header for job URL
     *
     * Only one mock even though Patient parameter requrests will be POST - TRD specifies that
     *   all requests will be POST to $shared-export
     */
    @Override
    public synchronized OneEcosystemExportResponse export(OneEcosystemExportRequest request) {
        final UUID jobId = UUID.randomUUID();
        final String jobUrl = "https://localhost:3002/api/v3/jobs/" + jobId;
        final List<String> resourceTypes = request.resourceTypes().isEmpty()
            ? List.of(ALL_RESOURCE_TYPES)
            : request.resourceTypes();
        jobs.put(jobId, new MockJob(jobId, resourceTypes));
        return new OneEcosystemExportResponse(202, jobUrl);
    }

    /**
     * arbitrarily 202's two times then returns response with where to get the file (200)
     */
    @Override
    public synchronized OneEcosystemJobStatus jobStatus(UUID jobId) {
        MockJob job = jobs.get(jobId);
        if (job == null) {
            return new OneEcosystemJobStatus(400, new OneEcosystemJobStatusResponse(
                Instant.now(),
                true,
                List.of(),
                List.of("error occurred on backend"),
                jobId
            ));
        }
        if (++job.polls <= POLL_RETRIES_BEFORE_FILE_DOWNLOAD) {
            return new OneEcosystemJobStatus(202, null);
        }
        if (!job.complete) {
            writeNdjsonFiles(job);
            job.complete = true;
        }
        return new OneEcosystemJobStatus(200, new OneEcosystemJobStatusResponse(
            Instant.now(),
            true,
            output(job),
            List.of(),
            job.jobId
        ));
    }

    private List<OneEcosystemJobStatusOutput> output(MockJob job) {
        return job.resourceTypes.stream()
            .map(resourceType -> new OneEcosystemJobStatusOutput(
                resourceType,
                "/data/" + job.jobId + "/" + job.jobId + "-" + resourceType + ".ndjson"
            ))
            .toList();
    }

    private void writeNdjsonFiles(MockJob job) {
        try {
            Files.createDirectories(exportPath);
            byte[] ndjson = "{\"foo\": \"bar\"}\n"
                .getBytes(StandardCharsets.UTF_8);

            for (String resourceType : job.resourceTypes) {
                String filename = job.jobId + "-" + resourceType + ".ndjson";

                // write unzipped
                try (FileOutputStream output = new FileOutputStream(exportPath + "/" + filename)) {
                    output.write(ndjson);
                }
                // write gzipped
                try (
                    FileOutputStream fos = new FileOutputStream(exportPath + "/" + filename + ".gz");
                    GZIPOutputStream output = new GZIPOutputStream(fos)
                ) {
                    output.write(ndjson);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write mock export file", e);
        }
    }

    private static class MockJob {
        private final UUID jobId;
        private final List<String> resourceTypes;
        private int polls;
        private boolean complete;

        private MockJob(UUID jobId, List<String> resourceTypes) {
            this.jobId = jobId;
            this.resourceTypes = List.copyOf(resourceTypes);
        }
    }
}
