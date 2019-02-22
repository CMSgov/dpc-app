package gov.cms.dpc.web.resources.v1;

import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.common.models.JobModel;
import gov.cms.dpc.queue.JobQueue;
import gov.cms.dpc.common.annotations.APIV1;
import gov.cms.dpc.web.resources.AbstractGroupResource;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


public class GroupResource extends AbstractGroupResource {

    private static final Logger logger = LoggerFactory.getLogger(GroupResource.class);

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
     * On success, returns a {@link org.eclipse.jetty.http.HttpStatus#NO_CONTENT_204} response along with a {@link org.hl7.fhir.r4.model.OperationOutcome} result.
     * The `Content-Location` header contains the URI to call when
     *
     * @param providerID {@link String} ID of provider to retrieve data for
     * @return - {@link org.hl7.fhir.r4.model.OperationOutcome} specifying whether or not the request was successful.
     */
    @Override
    @Path("/{providerID}/$export")
    @GET // Need this here, since we're using a path param
    public Response export(@PathParam("providerID") String providerID) {
        logger.debug("Exporting data for provider: {}", providerID);

        // Get a list of attributed beneficiaries
        final Optional<Set<String>> attributedBeneficiaries = this.client.getAttributedBeneficiaries(providerID);

        if (attributedBeneficiaries.isEmpty()) {
            throw new WebApplicationException(String.format("Unable to get attributed patients for provider: {}", providerID), Response.Status.NOT_FOUND);
        }

        // Generate a job ID and submit it to the queue
        final UUID jobID = UUID.randomUUID();

        this.queue.submitJob(jobID, new JobModel(providerID, attributedBeneficiaries.get()));

        return Response.status(Response.Status.NO_CONTENT)
                .contentLocation(URI.create(this.baseURL + "/Jobs/" + jobID)).build();
    }

    /**
     * Test method for verifying FHIR deserialization, it will eventually be removed.
     * TODO(nickrobison): Remove this
     *
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
}
