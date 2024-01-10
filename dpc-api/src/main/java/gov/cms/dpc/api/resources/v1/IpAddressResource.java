package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.entities.IpAddressEntity;
import gov.cms.dpc.api.jdbi.IpAddressDAO;
import gov.cms.dpc.api.models.CollectionResponse;
import gov.cms.dpc.api.models.CreateIpAddressRequest;
import gov.cms.dpc.api.resources.AbstractIpAddressResource;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.hypersistence.utils.hibernate.type.basic.Inet;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.UUID;

@Api(tags = {"Auth", "IpAddress"}, authorizations = @Authorization(value = "access_token"))
@Path("/v1/IpAddress")
public class IpAddressResource extends AbstractIpAddressResource {
    private static final Logger logger = LoggerFactory.getLogger(IpAddressResource.class);
    static final int MAX_IPS = 8;
    private final IpAddressDAO dao;

    @Inject
    public IpAddressResource(IpAddressDAO dao) {
        this.dao = dao;
    }

    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @ExceptionMetered
    @Authorizer
    @UnitOfWork
    @ApiOperation(
        value = "Fetch Ip addresses for an organization",
        authorizations = @Authorization(value = "access_token")
    )
    public CollectionResponse<IpAddressEntity> getOrganizationIpAddresses(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal) {
        return new CollectionResponse<>(this.dao.fetchIpAddresses(organizationPrincipal.getID()));
    }

    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @ExceptionMetered
    @Authorizer
    @UnitOfWork
    @ApiOperation(
            value = "Submits an Ip address for an organization",
            notes = "Organizations are currently limited to 8 Ip addresses.  If you attempt to submit more a 400 will be returned.",
            authorizations = @Authorization(value = "access_token")
    )
    @ApiResponses(@ApiResponse(code = 400, message = "Organization has too many Ip addresses."))
    public IpAddressEntity submitIpAddress(@ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal, @ApiParam CreateIpAddressRequest createIpAddressRequest) {
        CollectionResponse currentIps = getOrganizationIpAddresses(organizationPrincipal);

        if(currentIps.getCount() >= MAX_IPS) {
            logger.debug(String.format("Cannot add Ip for org: %s.  They are already at the max of %d.", organizationPrincipal.getID(), MAX_IPS));
            throw new WebApplicationException(String.format("Max Ips for organization reached: %d", MAX_IPS), Response.Status.BAD_REQUEST);
        } else {
            IpAddressEntity ipAddressEntity = new IpAddressEntity()
                .setOrganizationId(organizationPrincipal.getID())
                .setIpAddress(new Inet(createIpAddressRequest.getIpAddress()))
                .setLabel(createIpAddressRequest.getLabel());

            return this.dao.persistIpAddress(ipAddressEntity);
        }
    }

    @Override
    @DELETE
    @Path("/{ipAddressId}")
    @Timed
    @ExceptionMetered
    @Authorizer
    @UnitOfWork
    @ApiOperation(
            value = "Deletes an Ip address for an organization",
            authorizations = @Authorization(value = "access_token")
    )
    public Response deleteIpAddress(
            @ApiParam(hidden = true) @Auth OrganizationPrincipal organizationPrincipal,
            @ApiParam @NotNull @PathParam(value = "ipAddressId") UUID ipAddressId
    ) {
        CollectionResponse currentIps = getOrganizationIpAddresses(organizationPrincipal);
        Optional<IpAddressEntity> optionalIp = currentIps.getEntities().stream().filter(ip ->
            ((IpAddressEntity)ip).getId().equals(ipAddressId)).findFirst();

        if(!optionalIp.isEmpty()) {
            this.dao.deleteIpAddress(optionalIp.get());
            return Response.noContent().build();
        } else {
            logger.debug("Cannot delete Ip: %s for org: %s.  Ip address not found.");
            throw new WebApplicationException("Ip address not found", Response.Status.NOT_FOUND);
        }
    }
}
