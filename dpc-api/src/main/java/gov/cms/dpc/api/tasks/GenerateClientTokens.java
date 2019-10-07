package gov.cms.dpc.api.tasks;

import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.resources.v1.TokenResource;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.servlets.tasks.Task;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin task for creating a Golden macaroon which has superuser permissions in the application.
 * This should only ever be called once per environment.
 */
@Singleton
public class GenerateClientTokens extends Task {

    private static final Logger logger = LoggerFactory.getLogger(GenerateClientTokens.class);

    private final MacaroonBakery bakery;
    private final TokenResource resource;

    @Inject
    GenerateClientTokens(MacaroonBakery bakery, TokenResource resource) {
        super("generate-macaroon");
        this.bakery = bakery;
        this.resource = resource;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {

        final ImmutableCollection<String> organizationCollection = parameters.get("organization");
        if (organizationCollection.isEmpty()) {
            logger.warn("CREATING UNRESTRICTED MACAROON. ENSURE THIS IS OK");
            final Macaroon macaroon = bakery.createMacaroon(Collections.emptyList());
            output.write(macaroon.serialize(MacaroonVersion.SerializationVersion.V2_JSON));
        } else {
            final String organization = organizationCollection.asList().get(0);
            final Organization orgResource = new Organization();
            orgResource.setId(organization);
            final String organizationToken = this.resource
                    .createOrganizationToken(
                            new OrganizationPrincipal(orgResource),
                            UUID.fromString(organization),
                            null,
                            Optional.empty());

            output.write(organizationToken);

        }

    }
}
