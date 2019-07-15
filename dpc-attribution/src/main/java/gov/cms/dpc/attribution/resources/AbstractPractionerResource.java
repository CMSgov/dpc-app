package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/Practitioner")
@FHIR
public abstract class AbstractPractionerResource {

    protected AbstractPractionerResource() {
        // Not used
    }

    /**
     * FHIR search endpoint which allows querying providers with a given NPI
     *
     * @param providerNPI - {@link String} NPI to use for querying Provider database
     * @param organizationID
     * @return - {@link Bundle} of {@link Practitioner} resources matching search parameters
     */
    @GET
    public abstract Bundle getPractitioners(String providerNPI, String organizationID);

    /**
     * Register {@link Practitioner} with application
     *
     * @param provider = {@link Practitioner}
     * @return - {@link Practitioner} with additional metadata added by application
     */
    @POST
    public abstract Response submitProvider(Practitioner provider);

    /**
     * Fetch specific {@link Practitioner} resource
     * This is the ID of the resource, NOT the identifying NPI
     *
     * @param providerID - {@link UUID} provider ID
     * @return - {@link Practitioner}
     */
    @GET
    public abstract Practitioner getProvider(UUID providerID);

    /**
     * Remove {@link Practitioner} resource
     * This also removes all depending resource (e.g. attribution lists)
     *
     * @param providerID - {@link UUID} provider ID
     * @return - {@link Response}
     */
    @DELETE
    public abstract Response deleteProvider(UUID providerID);

    /**
     * Update {@link Practitioner} resource
     *
     * @param providerID - {@link UUID} provider ID
     * @param provider   - {@link Practitioner} resource to update
     * @return - {@link Practitioner} newly updated resource
     */
    @PUT
    public abstract Practitioner updateProvider(UUID providerID, Practitioner provider);
}
