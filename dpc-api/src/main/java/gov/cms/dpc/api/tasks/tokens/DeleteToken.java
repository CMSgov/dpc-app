package gov.cms.dpc.api.tasks.tokens;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.resources.v1.TokenResource;
import io.dropwizard.servlets.tasks.Task;
import org.hl7.fhir.dstu3.model.Organization;

import com.google.inject.Inject;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static gov.cms.dpc.api.tasks.TasksCommon.extractOrganization;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;

/**
 * Admin task to delete a {@link gov.cms.dpc.api.entities.TokenEntity} registered for a given {@link Organization}
 * <p>
 * This requires `organization` and `token` query parms.
 */
@Singleton
public class DeleteToken extends Task {

    private final TokenResource resource;

    @Inject
    public DeleteToken(TokenResource resource) {
        super("delete-token");
        this.resource = resource;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) {
        final Organization organization = extractOrganization(parameters);

        final List<String> tokenCollection = parameters.get("token");

        if (tokenCollection == null || tokenCollection.isEmpty()) {
            throw new BadRequestException("Must have token");
        }

        final String tokenID = tokenCollection.get(0);
        this.resource
                .deleteOrganizationToken(
                        new OrganizationPrincipal(organization),
                        UUID.fromString(tokenID));
    }
}
