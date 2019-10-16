package gov.cms.dpc.api.tasks;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.resources.v1.TokenResource;
import io.dropwizard.servlets.tasks.Task;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.util.UUID;

@Singleton
public class DeleteToken extends Task {

    private final TokenResource resource;

    @Inject
    DeleteToken(TokenResource resource) {
        super("delete-token");
        this.resource = resource;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
        final ImmutableCollection<String> organizationCollection = parameters.get("organization");

        if (organizationCollection.isEmpty()) {
            throw new WebApplicationException("Must have organization", Response.Status.BAD_REQUEST);
        }

        final ImmutableCollection<String> tokenCollection = parameters.get("token");

        if (tokenCollection.isEmpty()) {
            throw new WebApplicationException("Must have token", Response.Status.BAD_REQUEST);
        }
        final String organizationID = organizationCollection.asList().get(0);
        final String tokenID = tokenCollection.asList().get(0);

        final Organization organization = new Organization();
        organization.setId(organizationID);

        this.resource
                .deleteOrganizationToken(
                        new OrganizationPrincipal(organization),
                        UUID.fromString(organizationID),
                        UUID.fromString(tokenID));
    }
}
