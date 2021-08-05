package gov.cms.dpc.api.auth.macaroonauth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.google.inject.name.Named;
import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * Implementation of {@link Authenticator} which matches an {@link Organization} to the given Macaroon (base64 encoded string
 * If no {@link Organization} is found, this returns an empty optional, which signifies and authorization failure.
 */
public class MacaroonsAuthenticator implements Authenticator<DPCAuthCredentials, OrganizationPrincipal> {

    private static final Logger logger = LoggerFactory.getLogger(MacaroonsAuthenticator.class);

    private final IGenericClient client;

    @Inject
    public MacaroonsAuthenticator(@Named("attribution") IGenericClient client) {
        this.client = client;
    }

    @Override
    public Optional<OrganizationPrincipal> authenticate(DPCAuthCredentials credentials) {
        logger.debug("Performing token authentication");

        // If we don't have a path authorizer, just return the principal
        final OrganizationPrincipal principal = new OrganizationPrincipal(credentials.getOrganization());
        if (credentials.getPathAuthorizer() == null) {
            logger.debug("No path authorizer is present, returning principal");
            return Optional.of(principal);
        }

        // If we're an organization, we just check the org ID against the path value and see if it matches
        if (credentials.getPathAuthorizer().type() == DPCResourceType.Organization) {
            return validateOrganization(principal, credentials);
        }

        // Otherwise, try to lookup the matching resource
        logger.debug("Looking up resource {} in path authorizer. With value: {}", credentials.getPathAuthorizer().type(), credentials.getPathAuthorizer().pathParam());
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("_id", Collections.singletonList(credentials.getPathValue()));
        searchParams.put("organization", Collections.singletonList(credentials.getOrganization().getId()));

        // Special handling of Group resources, which use tags instead of resource properties.
        // TODO: Remove with DPC-552
        if (credentials.getPathAuthorizer().type() == DPCResourceType.Group) {
            searchParams.put("_tag", Collections.singletonList(String.format("%s|%s", DPCIdentifierSystem.DPC.getSystem(), credentials.getOrganization().getId())));
        }
        final Bundle bundle = this.client
                .search()
                .forResource(credentials.getPathAuthorizer().type().toString())
                .whereMap(searchParams)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (bundle.getTotal() == 0) {
            return Optional.empty();
        }

        return Optional.of(principal);
    }

    private Optional<OrganizationPrincipal> validateOrganization(OrganizationPrincipal principal, DPCAuthCredentials credentials) {
        final String orgID = credentials.getOrganization().getId();
        final String pathValue = credentials.getPathValue();
        logger.debug("Validating Organization {} matches path value: {}", orgID, pathValue);
        return orgID.equals("Organization/" + pathValue) ?
                Optional.of(principal) : Optional.empty();
    }
}
