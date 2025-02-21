package gov.cms.dpc.api.resources;

import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.annotations.FHIR;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.hl7.fhir.dstu3.model.StructureDefinition;

import java.util.List;

@Path("/StructureDefinition")
@FHIR
public abstract class AbstractDefinitionResource {

    protected AbstractDefinitionResource() {
        // Not used
    }

    @GET
    public abstract List<StructureDefinition> getStructureDefinitions();

    @GET
    public abstract StructureDefinition getStructureDefinition(@NoHtml String definitionID);
}
