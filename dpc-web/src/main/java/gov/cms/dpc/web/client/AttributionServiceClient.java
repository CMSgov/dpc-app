package gov.cms.dpc.web.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.web.DPWebConfiguration;
import gov.cms.dpc.web.annotations.AttributionService;
import org.eclipse.jetty.http.HttpStatus;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.Set;

@SuppressWarnings("unchecked")
public class AttributionServiceClient implements AttributionEngine {

    private final WebTarget client;
    private final DPWebConfiguration config;
    private final ObjectMapper mapper;

    @Inject
    public AttributionServiceClient(@AttributionService WebTarget client, DPWebConfiguration config) {
        this.client = client;
        this.config = config;
        this.mapper = new ObjectMapper();
    }

    @Override
    public Set<String> getAttributedBeneficiaries(String providerID) {
        final Invocation invocation = this.client
                .path(String.format("Group/%s", providerID))
                .request(FHIRMediaTypes.FHIR_JSON)
                .buildGet();
        try (Response response = invocation.invoke()) {
            if (!HttpStatus.isSuccess(response.getStatus())) {
                throw new WebApplicationException(response.getStatusInfo().getReasonPhrase(), HttpStatus.INTERNAL_SERVER_ERROR_500);
            }

            return (Set<String>) response.readEntity(Set.class);
        } catch (Exception e) {
            throw new WebApplicationException(e, HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }

    @Override
    public void addAttributionRelationship(String providerID, String beneficiaryID) {

        final Invocation invocation = this.client
                .path(String.format("Group/%s/%s", providerID, beneficiaryID))
                .request(FHIRMediaTypes.FHIR_JSON)
                .buildPut(Entity.json(null));
        handleNonBodyResponse(invocation);
    }

    @Override
    public void removeAttributionRelationship(String providerID, String beneficiaryID) {
        final Invocation invocation = this.client
                .path(String.format("Group/%s/%s", providerID, beneficiaryID))
                .request(FHIRMediaTypes.FHIR_JSON)
                .buildDelete();
        handleNonBodyResponse(invocation);
    }

    @Override
    public boolean isAttributed(String providerID, String beneficiaryID) {
        final Invocation invocation = this.client
                .path(String.format("Group/%s/%s", providerID, beneficiaryID))
                .request(FHIRMediaTypes.FHIR_JSON)
                .buildGet();

        try (Response response = invocation.invoke()) {
            if (!HttpStatus.isSuccess(response.getStatus())) {
                return false;
            }
        } catch (Exception e) {
            throw new WebApplicationException(e, HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
        return true;
    }

    /**
     * Helper method to handle throwing an exception is one exists.
     * Used for anything that doesn't return a usable body (such as PUT, or DELETE)
     *
     * @param invocation - {@link Invocation} call to make to remove service
     */
    private static void handleNonBodyResponse(Invocation invocation) {
        try (Response response = invocation.invoke()) {
            if (!HttpStatus.isSuccess(response.getStatus())) {
                throw new WebApplicationException(response.getStatusInfo().getReasonPhrase(), HttpStatus.INTERNAL_SERVER_ERROR_500);
            }
        } catch (Exception e) {
            throw new WebApplicationException(e, HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }

}
