package gov.cms.dpc.api.resources;

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

    @GET
    public abstract Bundle getPractitioners(String providerNPI);

    @POST
    public abstract Practitioner submitProvider(Practitioner provider);

    @GET
    public abstract Practitioner getProvider(UUID providerID);

    @DELETE
    public abstract Response deleteProvider(UUID providerID);

    @PUT
    public abstract Practitioner updateProvider(UUID providerID, Practitioner provider);
}
