package gov.cms.dpc.api.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.resources.v1.KeyResource;
import io.dropwizard.servlets.tasks.PostBodyTask;
import io.dropwizard.servlets.tasks.Task;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

@Singleton
public class UploadPublicKey extends PostBodyTask {

    private final KeyResource resource;
    private final ObjectMapper mapper;

    @Inject
    UploadPublicKey(KeyResource resource) {
        super("upload-key");
        this.resource = resource;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, String body, PrintWriter printWriter) throws IOException {
        final ImmutableCollection<String> organizationCollection = parameters.get("organization");

        if (organizationCollection.isEmpty()) {
            throw new WebApplicationException("Must have organization", Response.Status.BAD_REQUEST);
        }

        final String organizationID = organizationCollection.asList().get(0);
        final Organization orgResource = new Organization();
        orgResource.setId(organizationID);

        final PublicKeyEntity publicKeyEntity = resource.submitKey(new OrganizationPrincipal(orgResource), body, Optional.empty());

        this.mapper.writeValue(printWriter, publicKeyEntity);
    }
}
