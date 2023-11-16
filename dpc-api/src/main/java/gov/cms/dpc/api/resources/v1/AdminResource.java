package gov.cms.dpc.api.resources.v1;

import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.name.Named;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.auth.annotations.AdminOperation;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.hl7.fhir.dstu3.model.*;


@Api(value = "Admin", authorizations = @Authorization(value = "access_token"))
@Path("/v1/admin")
public class AdminResource {
    private final IGenericClient client;

    @Inject
    public AdminResource(@Named("attribution") IGenericClient client) {
        this.client = client;
    }

    @GET
    @Path("organizations")
    @FHIR
    @Timed
    @AdminOperation
    @ExceptionMetered
    @ApiOperation(value = "Get organizations by UUIDs",
            notes = "FHIR endpoint which returns list of Organization resources that are currently registered with the application.",
            authorizations = @Authorization(value = "access_token"))
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "An organization is only allowed to see their own Organization resource")})
    public Bundle getOrganizations(@NotNull @QueryParam(value="ids") Set<UUID> ids) {
        return this.client
                    .search()
                    .forResource(Organization.class)
                    .encodedJson()
                    .returnBundle(Bundle.class)
                    .execute();
    }
}
