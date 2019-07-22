package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractDefinitionResource;
import gov.cms.dpc.common.annotations.ServiceBaseURL;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StructureDefinition;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Api(value = "StructureDefinition")
public class DefinitionResource extends AbstractDefinitionResource {

    private final FhirContext ctx;
    private final DPCProfileSupport profileSupport;
    private final String serverURL;

    @Inject
    DefinitionResource(FhirContext ctx, DPCProfileSupport profileSupport, @ServiceBaseURL String serverURL) {
        this.ctx = ctx;
        this.profileSupport = profileSupport;
        this.serverURL = serverURL;
    }

    @Override
    @ApiOperation(value = "Fetch all structure definitions", notes = "FHIR endpoint which fetches all structure definitions from the server", response = Bundle.class)
    @FHIR
    public Bundle getStructureDefinitions() {
        final Bundle bundle = new Bundle();
        profileSupport.fetchAllStructureDefinitions(ctx)
                .forEach(structureDefinition -> bundle.addEntry().setResource(structureDefinition));

        return bundle;
    }

    @Override
    @GET
    @Path("/{definitionID}")
    @PathAuthorizer(type = ResourceType.PractitionerRole, pathParam = "definitionId")
    @FHIR
    @ApiOperation(value = "Fetch specific structure definition", notes = "FHIR endpoint to fetch a specific structure definition from the server.", response = StructureDefinition.class)
    public StructureDefinition getStructureDefinition(@PathParam("definitionID") String definitionID) {
        final StructureDefinition definition = this.profileSupport.fetchStructureDefinition(ctx, String.format("%s/StructureDefinition/%s", serverURL, definitionID));
        if (definition == null) {
            throw new WebApplicationException(String.format("Cannot find Structure Definition with ID: %s", definitionID), Response.Status.NOT_FOUND);
        }

        return definition;
    }
}
