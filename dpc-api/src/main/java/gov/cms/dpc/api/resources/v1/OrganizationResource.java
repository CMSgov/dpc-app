package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractOrganizationResource;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.swagger.annotations.*;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.UUID;

@Api(value = "Organization")
public class OrganizationResource extends AbstractOrganizationResource {

    private final IGenericClient client;

    @Inject
    public OrganizationResource(IGenericClient client) {
        this.client = client;
    }

    @Override
    @GET
    @Path("/{organizationID}")
    @FHIR
    @PathAuthorizer(type = ResourceType.Organization, pathParam = "organizationID")
    @ApiOperation(value = "Get organization details",
            notes = "This method returns the Organization resource that is currently registered with the application.",
    authorizations = @Authorization(value = "apiKey"))
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "An organization is only allowed to see their own Organization resource")})
    public Organization getOrganization(@PathParam("organizationID") UUID organizationID) {
        return this.client
                .read()
                .resource(Organization.class)
                .withId(organizationID.toString())
                .encodedJson()
                .execute();
    }
}
