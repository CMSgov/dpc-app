package gov.cms.dpc.api.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Bundle;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/Bundle")
@FHIR
public abstract class AbstractRosterResource {

    @POST
    public abstract Bundle submitRoster(Bundle providerBundle);
}
