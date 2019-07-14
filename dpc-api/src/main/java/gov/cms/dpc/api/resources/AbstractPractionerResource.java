package gov.cms.dpc.api.resources;

import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

//@Path("/Practitioner")
@FHIR
public abstract class AbstractPractionerResource {

    protected AbstractPractionerResource() {
        // Not used
    }

    public abstract Bundle getPractitioners(OrganizationPrincipal organization, String providerNPI);

    public abstract Practitioner submitProvider(OrganizationPrincipal organization, Practitioner provider);

//    @GET
//    @Path("/{providerID}")
    public abstract Practitioner getProvider(UUID providerID);

//    @DELETE
//    @Path("/{providerID}")
    public abstract Response deleteProvider(UUID providerID);

//    @PUT
//    @Path("/{providerID}")
    public abstract Practitioner updateProvider(UUID providerID, Practitioner provider);
}
