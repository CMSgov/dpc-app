package gov.cms.dpc.api.tasks.keys;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.resources.v1.KeyResource;
import io.dropwizard.servlets.tasks.Task;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static gov.cms.dpc.api.tasks.TasksCommon.extractOrganization;

/**
 * Admin task to delete a {@link gov.cms.dpc.api.entities.PublicKeyEntity} registered for the given {@link Organization}.
 * <p>
 * This requires `organization` and `key` query params.
 */
@Singleton
public class DeletePublicKey extends Task {

    private final KeyResource resource;

    @Inject
    public DeletePublicKey(KeyResource resource) {
        super("delete-key");
        this.resource = resource;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter printWriter) throws Exception{
        final Organization organization = extractOrganization(parameters);

        final List<String> keyCollection = parameters.get("key");

        if (keyCollection.isEmpty()) {
            throw new WebApplicationException("Must have key", Response.Status.BAD_REQUEST);
        }

        final String keyID = keyCollection.get(0);
        this.resource
                .deletePublicKey(new OrganizationPrincipal(organization),
                        UUID.fromString(keyID));
    }
}
