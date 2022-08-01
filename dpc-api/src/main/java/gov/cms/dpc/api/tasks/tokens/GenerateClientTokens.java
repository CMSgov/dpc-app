package gov.cms.dpc.api.tasks.tokens;

import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.entities.TokenEntity;
import gov.cms.dpc.api.resources.v1.TokenResource;
import gov.cms.dpc.api.resources.v1.OrganizationResource;
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
import java.util.Optional;

/**
 * Admin task for creating a Golden macaroon which has superuser permissions in the application.
 * This should only ever be called once per environment.
 */
@Singleton
public class GenerateClientTokens extends Task {

    private static final Logger logger = LoggerFactory.getLogger(GenerateClientTokens.class);

    private final MacaroonBakery bakery;
    private final TokenResource resourceToken;
    private final OrganizationResource resourceOrganization;

    @Inject
    public GenerateClientTokens(MacaroonBakery bakery, TokenResource resourceToken, OrganizationResource  resourceOrganization) {
        super("generate-token");
        this.bakery = bakery;
        this.resourceToken = resourceToken;
        this.resourceOrganization = resourceOrganization;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) {
        final ImmutableCollection<String> expirationCollection = parameters.get("expiration");
        final ImmutableCollection<String> labelCollection = parameters.get("label");
        final ImmutableCollection<String> organizationCollection = parameters.get("organization");
        if (organizationCollection.isEmpty()) {
            logger.warn("CREATING UNRESTRICTED MACAROON. ENSURE THIS IS OK");
            final Macaroon macaroon = bakery.createMacaroon(Collections.emptyList());
            output.write(macaroon.serialize(MacaroonVersion.SerializationVersion.V1_BINARY));
        } else {
            final String organizationId = organizationCollection.asList().get(0);
            final Organization orgResource = new Organization();
            orgResource.setId(organizationId);

            final OrganizationPrincipal orgPrincipal = new OrganizationPrincipal(orgResource);
            final var existingOrg = resourceOrganization.orgSearch(orgPrincipal);

            String existingId = existingOrg == null ? "-1" : existingOrg.getId();
            if(existingId == organizationId) {
                final String tokenLabel = labelCollection.isEmpty() ? null : labelCollection.asList().get(0);
                Optional<OffsetDateTimeParam> expiration = Optional.empty();
                if(!expirationCollection.isEmpty() && !StringUtils.isBlank(expirationCollection.asList().get(0))){
                    expiration = Optional.of(new OffsetDateTimeParam(expirationCollection.asList().get(0)));
                }
                final TokenEntity tokenResponse = this.resourceToken
                        .createOrganizationToken(
                                new OrganizationPrincipal(orgResource), null,
                                tokenLabel,
                                expiration);
    
                output.write(tokenResponse.getToken());
            } else {
                logger.warn("ATTEMPT TO CREATE ORPHAN MACAROON.");
                throw new Error("ERROR: No Organization found with this ID (`" + organizationId + "`). Please double check your data and try again.");
            }
        }
    }
}