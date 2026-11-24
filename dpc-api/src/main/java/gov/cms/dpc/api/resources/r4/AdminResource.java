package gov.cms.dpc.api.resources.r4;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.annotations.AdminOperation;
import gov.cms.dpc.attribution.client.AttributionClient;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.swagger.annotations.*;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.hl7.fhir.r4.model.Bundle;

@FHIR
@Api(value = "Admin", authorizations = @Authorization(value = "access_token"))
public class AdminResource {
    private final AttributionClient attributionClient;

    @Inject
    public AdminResource(AttributionClient attributionClient) {
        this.attributionClient = attributionClient;
    }

    @GET
    @Path("Organization")
    @FHIR
    @Timed
    @AdminOperation
    @ExceptionMetered
    @ApiOperation(value = "Get organizations by NPIs",
            notes = "FHIR endpoint which returns list of Organization resources that are currently registered with the application.",
            authorizations = @Authorization(value = "access_token"))
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Only administrators can use this endpoint")})
    public Bundle getOrganizations(@NotNull @QueryParam(value="npis") String npis) {
        return this.attributionClient
                .searchOrganizations(npis);
    }
}
