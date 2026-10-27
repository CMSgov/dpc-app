package gov.cms.dpc.newbe.client;

import gov.cms.dpc.newbe.models.NewbeExportRequest;
import gov.cms.dpc.newbe.models.NewbeExportResponse;
import gov.cms.dpc.newbe.models.NewbeJobStatus;
import gov.cms.dpc.newbe.models.NewbeJobStatusOutput;
import gov.cms.dpc.newbe.models.NewbeJobStatusResponse;

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

public class MockNewbeClient implements NewbeClient {

    private static final int POLL_RETRIES_BEFORE_FILE_DOWNLOAD = 2;
    private static final String[] ALL_RESOURCE_TYPES = {"Patient", "Coverage", "ExplanationOfBenefit"};

    private final Path exportPath;
    private final Map<UUID, MockJob> jobs = new HashMap<>();

    public MockNewbeClient(Path exportPath) {
        this.exportPath = exportPath;
    }

    /**
     *  CapabilitiesStatement is retrurned from metadata endpoint
     *  <a href="https://bcda.cms.gov/api-documentation/access-claims-data.html#check-api-status-2">...</a>
     */
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
    public NewbeExportResponse export(NewbeExportRequest request) {
        final UUID jobId = UUID.randomUUID();
        final String requestUrl = "/v3/Group/" + request.groupId() + "/$export";
        final String jobUrl = "https://localhost:3002/api/v3/jobs/" + jobId;
        final List<String> resourceTypes = request.resourceTypes().isEmpty()
            ? List.of(ALL_RESOURCE_TYPES)
            : request.resourceTypes();
        jobs.put(jobId, new MockJob(jobId, requestUrl, resourceTypes));
        return new NewbeExportResponse(202, jobUrl);
    }

    /**
     * arbitrarily 202's two times then returns response with where to get the file (200)
     */
    @Override
    public synchronized NewbeJobStatus jobStatus(UUID jobId) {
        MockJob job = jobs.get(jobId);
        if (job == null) {
            return null;
        }
        if (++job.polls <= POLL_RETRIES_BEFORE_FILE_DOWNLOAD) {
            return new NewbeJobStatus(202, null);
        }
        if (!job.complete) {
            writeNdjsonFiles(job);
            job.complete = true;
        }
        return new NewbeJobStatus(200, new NewbeJobStatusResponse(
            Instant.now(),
            job.requestUrl,
            true,
            output(job),
            List.of(),
            job.jobId
        ));
    }

    private List<NewbeJobStatusOutput> output(MockJob job) {
        return job.resourceTypes.stream()
            .map(resourceType -> new NewbeJobStatusOutput(
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
        private final String requestUrl;
        private final List<String> resourceTypes;
        private int polls;
        private boolean complete;

        private MockJob(UUID jobId, String requestUrl, List<String> resourceTypes) {
            this.jobId = jobId;
            this.requestUrl = requestUrl;
            this.resourceTypes = List.copyOf(resourceTypes);
        }
    }
}
