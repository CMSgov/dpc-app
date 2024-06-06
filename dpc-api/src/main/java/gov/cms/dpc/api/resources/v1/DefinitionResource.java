package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.common.annotations.Public;
import gov.cms.dpc.api.resources.AbstractDefinitionResource;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import io.swagger.annotations.*;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.StructureDefinition;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;

@Api(value = "StructureDefinition")
@Path("/v1/StructureDefinition")
public class DefinitionResource extends AbstractDefinitionResource {

    private final FhirContext ctx;
    private final DPCProfileSupport profileSupport;

    @Inject
    DefinitionResource(FhirContext ctx, DPCProfileSupport profileSupport) {
        this.ctx = ctx;
        this.profileSupport = profileSupport;
    }

    @Public
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch all structure definitions", notes = "FHIR endpoint which fetches all structure definitions from the server", response = Bundle.class)
    @Override
    public List<StructureDefinition> getStructureDefinitions() {
        return profileSupport.fetchAllStructureDefinitions(ctx);
    }

    @Override
    @GET
    @Path("/{definitionID}")
    @Public
    @FHIR
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "Fetch specific structure definition", notes = "FHIR endpoint to fetch a specific structure definition from the server.", response = StructureDefinition.class)
    @ApiResponses(@ApiResponse(code = 404, message = "Unable to find Structure Definition"))
    public StructureDefinition getStructureDefinition(@ApiParam(value = "Structure Definition Resource ID", required = true) @PathParam("definitionID") @NoHtml String definitionID) {
        // The canonicalURL comes from the profile itself, which is always set to the production endpoint
        final String canonicalURL = String.format("https://dpc.cms.gov/api/v1/StructureDefinition/%s", definitionID);
        final StructureDefinition definition = this.profileSupport.fetchStructureDefinition(ctx, canonicalURL);
        if (definition == null) {
            throw new WebApplicationException(String.format("Cannot find Structure Definition with ID: %s", definitionID), Response.Status.NOT_FOUND);
        }

        return definition;
    }
}
