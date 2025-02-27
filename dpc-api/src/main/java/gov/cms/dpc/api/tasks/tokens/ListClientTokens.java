package gov.cms.dpc.api.tasks.tokens;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.resources.v1.TokenResource;
import io.dropwizard.servlets.tasks.Task;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static gov.cms.dpc.api.tasks.TasksCommon.extractOrganization;

/**
 * Admin task for listing {@link TokenEntity}s registered with the given {@link Organization}
 * <p>
 * This requries an `organization` query param.
 */
@Singleton
public class ListClientTokens extends Task {

    private final TokenResource resource;
    private final ObjectMapper mapper;

    @Inject
    public ListClientTokens(TokenResource resource) {
        super("list-tokens");
        this.resource = resource;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
        final Organization organization = extractOrganization(parameters);

        final CollectionResponse<TokenEntity> organizationTokens = this.resource.getOrganizationTokens(
                new OrganizationPrincipal(organization));
        this.mapper.writeValue(output, organizationTokens);
    }
}
