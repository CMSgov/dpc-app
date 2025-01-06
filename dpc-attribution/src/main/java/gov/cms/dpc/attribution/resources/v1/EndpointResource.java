package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.jdbi.EndpointDAO;
import gov.cms.dpc.attribution.jdbi.OrganizationDAO;
import gov.cms.dpc.attribution.resources.AbstractEndpointResource;
import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Endpoint;

import java.util.List;
import java.util.UUID;

public class EndpointResource extends AbstractEndpointResource {

    private final EndpointDAO endpointDAO;
    private final OrganizationDAO organizationDAO;
    private final FHIREntityConverter converter;

    @Inject
    public EndpointResource(FHIREntityConverter converter, EndpointDAO endpointDAO, OrganizationDAO organizationDAO) {
        this.converter = converter;
        this.endpointDAO = endpointDAO;
        this.organizationDAO = organizationDAO;
    }

    @FHIR
    @POST
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @Override
    public Response createEndpoint(@NotNull Endpoint endpoint) {
        EndpointEntity entity = endpointDAO.persistEndpoint(converter.fromFHIR(EndpointEntity.class, endpoint));
        return Response.status(Response.Status.CREATED).entity(converter.toFHIR(Endpoint.class, entity)).build();
    }

    @FHIR
    @GET
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @Override
    public List<Endpoint> searchEndpoints(@NotNull @QueryParam("organization") String organizationID, @QueryParam("_id") String resourceId) {
        final UUID orgUUID = FHIRExtractors.getEntityUUID(organizationID);
        UUID endpointUUID = null;
        if(resourceId!=null){
            endpointUUID = FHIRExtractors.getEntityUUID(resourceId);
        }

        final List<EndpointEntity> endpointList = this.endpointDAO.endpointSearch(orgUUID, endpointUUID);

        return endpointList
                .stream()
                .map(e -> converter.toFHIR(Endpoint.class, e))
                .toList();
    }

    @FHIR
    @GET
    @Path("/{endpointID}")
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @Override
    public Endpoint fetchEndpoint(@NotNull @PathParam("endpointID") UUID endpointID) {
        final EndpointEntity endpoint = this.endpointDAO.fetchEndpoint(endpointID)
                .orElseThrow(() -> new WebApplicationException("Unable to find Endpoint", Response.Status.NOT_FOUND));

        return converter.toFHIR(Endpoint.class, endpoint);
    }

    @FHIR
    @PUT
    @Path("/{endpointID}")
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @Override
    public Endpoint updateEndpoint(@NotNull @PathParam("endpointID") UUID endpointID, @NotNull Endpoint endpoint) {
        EndpointEntity updatedEntity = this.endpointDAO.persistEndpoint(converter.fromFHIR(EndpointEntity.class, endpoint));
        return converter.toFHIR(Endpoint.class, updatedEntity);
    }

    @FHIR
    @DELETE
    @Path("/{endpointID}")
    @Timed
    @ExceptionMetered
    @UnitOfWork
    @Override
    public Response deleteEndpoint(@NotNull @PathParam("endpointID") UUID endpointID) {
        final EndpointEntity endpoint = this.endpointDAO.fetchEndpoint(endpointID)
                .orElseThrow(() -> new WebApplicationException("Unable to find Endpoint", Response.Status.NOT_FOUND));
        OrganizationEntity organization = endpoint.getOrganization();
        List<EndpointEntity> endpoints = organization.getEndpoints();
        if (endpoints.size() == 1) {
            throw new WebApplicationException("Cannot delete Organization's only endpoint", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }

        endpoints.removeIf(e -> endpointID.equals(e.getId()));
        organization.setEndpoints(endpoints);
        this.organizationDAO.updateOrganization(organization.getId(), organization);
        this.endpointDAO.deleteEndpoint(endpoint);

        return Response.status(Response.Status.OK).build();
    }
}
