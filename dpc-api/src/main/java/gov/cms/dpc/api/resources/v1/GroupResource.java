package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.resources.AbstractGroupResource;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRBuilders;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.queue.models.JobModel;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

    // The delimiter for the '_types' list query param.
    public static final String LIST_DELIM = ",";

    private final JobQueue queue;
    private final AttributionEngine client;
    private final String baseURL;

    @Inject
    public GroupResource(JobQueue queue, AttributionEngine client, @APIV1 String baseURL) {
        this.queue = queue;
        this.client = client;
        this.baseURL = baseURL;
    }

    /**
     * Begin export process for the given provider
     * On success, returns a {@link org.eclipse.jetty.http.HttpStatus#NO_CONTENT_204} response along with a {@link org.hl7.fhir.dstu3.model.OperationOutcome} result.
     * The `Content-Location` header contains the URI to call when checking job status.
     *
     * @param providerID    {@link String} ID of provider to retrieve data for
     * @param resourceTypes - {@link String} of comma separated values corresponding to FHIR {@link ResourceType}
     * @return - {@link org.hl7.fhir.dstu3.model.OperationOutcome} specifying whether or not the request was successful.
     */
    @Override
    @Timed
    @Path("/{providerID}/$export")
    @GET // Need this here, since we're using a path param
    public Response export(@PathParam("providerID") String providerID, @QueryParam("_type") String resourceTypes) {
        logger.debug("Exporting data for provider: {}", providerID);

        // Get a list of attributed beneficiaries
        final Optional<List<String>> attributedBeneficiaries = this.client.getAttributedPatientIDs(FHIRBuilders.buildPractitionerFromNPI(providerID));

        if (attributedBeneficiaries.isEmpty()) {
            throw new WebApplicationException(String.format("Unable to get attributed patients for provider: %s", providerID), Response.Status.NOT_FOUND);
        }

        // Generate a job ID and submit it to the queue
        final UUID jobID = UUID.randomUUID();

        // Handle the _type query parameter
        final var resources = handleTypeQueryParam(resourceTypes);

        this.queue.submitJob(jobID, new JobModel(jobID, resources, providerID, attributedBeneficiaries.get()));

        return Response.status(Response.Status.NO_CONTENT)
                .contentLocation(URI.create(this.baseURL + "/Jobs/" + jobID)).build();
    }

    /**
     * Test method for verifying FHIR deserialization, it will eventually be removed.
     * TODO(nickrobison): Remove this
     *
     * @param group - {@link Group} to use for testing
     * @return - {@link String} test string
     */
    @POST
    public Patient marshalTest(Group group) {

        if (group.getIdentifierFirstRep().getValue().equals("Group/fail")) {
            throw new IllegalStateException("Should fail");
        }

        final HumanName name = new HumanName().setFamily("Doe").addGiven("John");
        return new Patient().addName(name).addIdentifier(new Identifier().setValue("test-id"));
    }

    /**
     * Convert the '_types' {@link QueryParam} to a list of resources to add to the job. Handle the empty case,
     * by returning all valid resource types.
     *
     * @param resourcesListParam - {@link String} of comma separated values corresponding to FHIR {@link ResourceType}s
     * @return - A list of {@link ResourceType} to return for this request.
     */
    private List<ResourceType> handleTypeQueryParam(String resourcesListParam) {
        // If the query param is omitted, the FHIR spec states that all resources should be returned
        if (resourcesListParam == null || resourcesListParam.isEmpty()) {
            return JobModel.validResourceTypes;
        }

        final var resources = new ArrayList<ResourceType>();
        for (String queryResource : resourcesListParam.split(LIST_DELIM, -1)) {
            final var foundResourceType = matchResourceType(queryResource);
            if (foundResourceType.isEmpty()) {
                throw new WebApplicationException(String.format("Unsupported resource name in the '_type' query parameter: %s", queryResource), Response.Status.BAD_REQUEST);
            }
            resources.add(foundResourceType.get());
        }
        return resources;
    }

    /**
     * Convert a single resource type in a query param into a {@link ResourceType}.
     *
     * @param queryResourceType - The text from the query param
     * @return If match is found a {@link ResourceType}
     */
    private Optional<ResourceType> matchResourceType(String queryResourceType) {
        final var canonical = queryResourceType.trim().toUpperCase();

        // Implementation Note: resourceTypeMap is a small list <3 so hashing isn't faster
        return JobModel.validResourceTypes.stream()
                .filter(validResource -> validResource.toString().equalsIgnoreCase(canonical))
                .findFirst();
    }
}
