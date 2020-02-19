package gov.cms.dpc.api.tasks.keys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.resources.v1.KeyResource;
import gov.cms.dpc.api.tasks.TasksCommon;
import io.dropwizard.servlets.tasks.PostBodyTask;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.util.Optional;

/**
 * Admin task to upload a PEM encoded public key for the given {@link Organization}.
 * <p>
 * This requires `organization` query param and optionally a `label` for specifying a custom key label.
 * The PEM encoded key is sent as the body of the POST command.
 */
@Singleton
public class UploadPublicKey extends PostBodyTask {

    private final KeyResource resource;
    private final ObjectMapper mapper;

    @Inject
    public UploadPublicKey(KeyResource resource) {
        super("upload-key");
        this.resource = resource;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, String body, PrintWriter output) throws Exception {
        final Organization organization = TasksCommon.extractOrganization(parameters);
        final ImmutableCollection<String> labelParams = parameters.get("label");

        final Optional<String> label;
        if (labelParams.isEmpty()) {
            label = Optional.empty();
        } else {
            label = Optional.ofNullable(labelParams.asList().get(0));
        }

        final PublicKeyEntity publicKeyEntity = this.resource.submitKey(new OrganizationPrincipal(organization), body, label);
        this.mapper.writeValue(output, publicKeyEntity);
    }
}
