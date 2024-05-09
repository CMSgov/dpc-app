package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.annotations.Public;
import gov.cms.dpc.api.resources.AbstractDefinitionResource;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import org.hl7.fhir.dstu3.model.StructureDefinition;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;

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
    public StructureDefinition getStructureDefinition(@PathParam("definitionID") @NoHtml String definitionID) {
        // The canonicalURL comes from the profile itself, which is always set to the production endpoint
        final String canonicalURL = String.format("https://dpc.cms.gov/api/v1/StructureDefinition/%s", definitionID);
        final StructureDefinition definition = this.profileSupport.fetchStructureDefinition(ctx, canonicalURL);
        if (definition == null) {
            throw new WebApplicationException(String.format("Cannot find Structure Definition with ID: %s", definitionID), Response.Status.NOT_FOUND);
        }

        return definition;
    }
}
