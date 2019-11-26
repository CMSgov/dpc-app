package gov.cms.dpc.api.resources;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.StructureDefinition;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/StructureDefinition")
@FHIR
public abstract class AbstractDefinitionResource {

    protected AbstractDefinitionResource() {
        // Not used
    }

    @GET
    public abstract Bundle getStructureDefinitions();

    @GET
    public abstract StructureDefinition getStructureDefinition(String definitionID);
}
