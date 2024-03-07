package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.name.Named;
import gov.cms.dpc.api.auth.annotations.AdminOperation;
import gov.cms.dpc.api.resources.AbstractAdminResource;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.swagger.annotations.*;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Api(value = "Admin", authorizations = @Authorization(value = "access_token"))
@Path("/v1/Admin")
public class AdminResource extends AbstractAdminResource{
    private final IGenericClient client;

    @Inject
    public AdminResource(@Named("attribution") IGenericClient client) {
        this.client = client;
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
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("identifier", Collections.singletonList(npis));
        Bundle bundle = this.client
                    .search()
                    .forResource(Organization.class)
                    .whereMap(searchParams)
                    .encodedJson()
                    .returnBundle(Bundle.class)
                    .execute();
        return bundle;
    }
}
