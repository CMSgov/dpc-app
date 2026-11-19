package gov.cms.dpc.api.auth.macaroonauth.r4;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.fhir.DPCResourceType;
import io.dropwizard.auth.Authenticator;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.Optional;
import java.util.UUID;

public class MacaroonsAuthenticator implements Authenticator<DPCAuthCredentials, OrganizationPrincipal> {

    private final AttributionClient client;

    @Inject
    public MacaroonsAuthenticator(AttributionClient client) {
        this.client = client;
    }

    @Override
    public Optional<OrganizationPrincipal> authenticate(DPCAuthCredentials credentials) {
        final OrganizationPrincipal principal = new OrganizationPrincipal(credentials.getOrganization());
        final PathAuthorizer pathAuthorizer = credentials.getPathAuthorizer();
        if (pathAuthorizer == null) {
            return Optional.of(principal);
        }

        if (pathAuthorizer.type() == DPCResourceType.Organization) {
            return credentials.getOrganization().getId().equals("Organization/" + credentials.getPathValue())
                    ? Optional.of(principal) : Optional.empty();
        }

        if (pathAuthorizer.type() == DPCResourceType.Practitioner) {
            final UUID practitionerId = UUID.fromString(credentials.getPathValue());
            if (client.searchPractitioners(principal.getID(), practitionerId).getEntry().isEmpty()) {
                throw new NotFoundException("Practitioner not found");
            }
            return Optional.of(principal);
        }

        throw new IllegalStateException("No v3 path authorization for " + pathAuthorizer.type());
    }
}
