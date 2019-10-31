package gov.cms.dpc.api.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.resources.v1.TokenResource;
import io.dropwizard.servlets.tasks.Task;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;

@Singleton
public class ListClientTokens extends Task {

    private final TokenResource resource;
    private final ObjectMapper mapper;

    @Inject
    ListClientTokens(TokenResource resource) {
        super("list-tokens");
        this.resource = resource;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
        final ImmutableCollection<String> organizationCollection = parameters.get("organization");

        if (organizationCollection.isEmpty()) {
            throw new WebApplicationException("Must have organization", Response.Status.BAD_REQUEST);
        }

        final String organizationID = organizationCollection.asList().get(0);
        final Organization orgResource = new Organization();
        orgResource.setId(organizationID);

        final List<TokenEntity> organizationTokens = this.resource.getOrganizationTokens(
                new OrganizationPrincipal(orgResource));

        this.mapper.writeValue(output, organizationTokens);
    }
}
