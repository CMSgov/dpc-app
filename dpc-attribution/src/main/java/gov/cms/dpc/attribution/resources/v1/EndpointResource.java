package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.jdbi.EndpointDAO;
import gov.cms.dpc.attribution.resources.AbstractEndpointResource;
import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.entities.EndpointEntityConverter;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Api(value = "Endpoint")
public class EndpointResource extends AbstractEndpointResource {

    private final EndpointDAO endpointDAO;

    @Inject
    public EndpointResource(EndpointDAO endpointDAO) {
        this.endpointDAO = endpointDAO;
    }

    @FHIR
    @GET
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @ApiOperation(value = "Search Endpoints", notes = "Search for Endpoints associated to the given Organization")
    @Override
    public Bundle searchEndpoints(@NotNull @QueryParam("organization") String organizationID) {
        final UUID entityID = FHIRExtractors.getEntityUUID(organizationID);

        final List<EndpointEntity> endpointList = this.endpointDAO.findByOrganization(entityID);

        final Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);

        endpointList
                .stream()
                .map(EndpointEntityConverter::convert)
                .forEach(endpoint -> bundle.addEntry().setResource(endpoint));

        bundle.setTotal(endpointList.size());

        return bundle;
    }

    @FHIR
    @GET
    @Path("/{endpointID}")
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @ApiOperation(value = "Fetch endpoint", notes = "Fetch a specific Endpoint by resource ID")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Endpoint"))
    @Override
    public Endpoint fetchEndpoint(@PathParam("endpointID") UUID endpointID) {
        final EndpointEntity endpoint = this.endpointDAO.fetchEndpoint(endpointID)
                .orElseThrow(() -> new WebApplicationException("Unable to find Endpoint", Response.Status.NOT_FOUND));

        return EndpointEntityConverter.convert(endpoint);
    }
}
