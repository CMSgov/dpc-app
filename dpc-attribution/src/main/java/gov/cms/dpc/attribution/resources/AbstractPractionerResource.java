package gov.cms.dpc.attribution.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;
import java.util.UUID;

@Path("/Practitioner")
@FHIR
public abstract class AbstractPractionerResource {

    protected AbstractPractionerResource() {
        // Not used
    }

    @GET
    public abstract List<Practitioner> getPractitioners(String providerNPI);

    @POST
    public abstract Practitioner submitProvider(Practitioner provider);

    @GET
    public abstract Practitioner getProvider(UUID providerID);
}
