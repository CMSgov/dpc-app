package gov.cms.dpc.api.tasks.tokens;

import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.resources.v1.TokenResource;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.jersey.jsr310.OffsetDateTimeParam;
import io.dropwizard.servlets.tasks.Task;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public GenerateClientTokens(MacaroonBakery bakery, TokenResource resource) {
        super("generate-token");
        this.bakery = bakery;
        this.resource = resource;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
        final List<String> expirationCollection = parameters.get("expiration");
        final List<String> labelCollection = parameters.get("label");
        final List<String> organizationCollection = parameters.get("organization");
        if (organizationCollection == null || organizationCollection.isEmpty()) {
            logger.warn("CREATING UNRESTRICTED MACAROON. ENSURE THIS IS OK");
            final Macaroon macaroon = bakery.createMacaroon(Collections.emptyList());
            output.write(macaroon.serialize(MacaroonVersion.SerializationVersion.V1_BINARY));
        } else {
            final String organization = organizationCollection.get(0);
            final Organization orgResource = new Organization();
            orgResource.setId(organization);
            final String tokenLabel = (labelCollection == null || labelCollection.isEmpty()) ? null : labelCollection.get(0);
            Optional<OffsetDateTimeParam> expiration = Optional.empty();

            if(expirationCollection != null && !expirationCollection.isEmpty() && !StringUtils.isBlank(expirationCollection.get(0))){
                expiration = Optional.of(new OffsetDateTimeParam(expirationCollection.get(0)));
            }
            final TokenEntity tokenResponse = this.resource
                    .createOrganizationToken(
                            new OrganizationPrincipal(orgResource), null,
                            tokenLabel,
                            expiration);

            output.write(tokenResponse.getToken());
        }
    }
}