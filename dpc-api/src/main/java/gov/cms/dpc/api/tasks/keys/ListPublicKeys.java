package gov.cms.dpc.api.tasks.keys;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.resources.v1.KeyResource;
import io.dropwizard.servlets.tasks.Task;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static gov.cms.dpc.api.tasks.TasksCommon.extractOrganization;

/**
 * Admin task to list {@link PublicKeyEntity} for a given {@link Organization}
 * <p>
 * This requires 'organization' query params.
 */
@Singleton
public class ListPublicKeys extends Task {

    private final KeyResource resource;
    private final ObjectMapper mapper;

    @Inject
    public ListPublicKeys(KeyResource resource) {
        super("list-keys");
        this.resource = resource;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
        final Organization organization = extractOrganization(parameters);

        final CollectionResponse<PublicKeyEntity> publicKeys = this.resource.getPublicKeys(new OrganizationPrincipal(organization));
        this.mapper.writeValue(output, publicKeys);
    }
}
