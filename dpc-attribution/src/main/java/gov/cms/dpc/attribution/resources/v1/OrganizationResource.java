package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.jdbi.EndpointDAO;
import gov.cms.dpc.attribution.jdbi.OrganizationDAO;
import gov.cms.dpc.attribution.resources.AbstractOrganizationResource;
import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.FHIRParameter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static gov.cms.dpc.attribution.utils.RESTUtils.parseTokenTag;

public class OrganizationResource extends AbstractOrganizationResource {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationResource.class);

    private final OrganizationDAO dao;
    private final EndpointDAO endpointDAO;
    private final FHIREntityConverter converter;
    private final DPCAttributionConfiguration config;
    private final Supplier<UUID> uuidSupplier = UUID::randomUUID;

    @Inject
    OrganizationResource(FHIREntityConverter converter, OrganizationDAO dao, EndpointDAO endpointDAO, DPCAttributionConfiguration config) {
        this.converter = converter;
        this.dao = dao;
        this.endpointDAO = endpointDAO;
        this.config = config;
    }

    @Override
    @GET
    @FHIR
    @UnitOfWork
    public List<Organization> searchOrganizations(
            @QueryParam("identifier") String identifier) {

        if (identifier == null) {
            return this.dao.listOrganizations()
                    .stream()
                    .map(o -> this.converter.toFHIR(Organization.class, o))
                    .collect(Collectors.toList());
        }
        String parsedToken = parseTokenTag((tag) -> tag, identifier);
        Set<String> idSet = Arrays.asList(parsedToken.split(",")).stream().collect(Collectors.toSet());
        if (idSet.size() > 1) {
            return this.dao.getOrganizationsByIds(idSet)
                .stream()
                .map(o -> this.converter.toFHIR(Organization.class, o))
                .collect(Collectors.toList());
        }
        // Pull out the NPI, keeping it as a string.
        final List<OrganizationEntity> queryList = this.dao.searchByIdentifier(parsedToken);

        return queryList
                .stream()
                .map(o -> this.converter.toFHIR(Organization.class, o))
                .collect(Collectors.toList());
    }

    @Override
    @FHIR
    @UnitOfWork
    @Timed
    @ExceptionMetered
    public Response submitOrganization(@FHIRParameter(name = "resource") Bundle transactionBundle) {

        final Optional<Organization> optOrganization = transactionBundle
                .getEntry()
                .stream()
                .filter(entry -> entry.hasResource() && entry.getResource().getResourceType().getPath().equals(DPCResourceType.Organization.getPath()))
                .map(entry -> (Organization) entry.getResource())
                .findFirst();

        if (optOrganization.isEmpty()) {
            return Response.status(HttpStatus.UNPROCESSABLE_ENTITY_422).entity("Must provide organization to register").build();
        }

        Organization organization = optOrganization.get();
        if (StringUtils.isBlank(organization.getId())) {
            organization.setId(generateNewOrgId());
        }
        final OrganizationEntity entity = this.converter.fromFHIR(OrganizationEntity.class, organization);
        final List<EndpointEntity> endpoints = extractEndpoints(transactionBundle);
        endpoints.forEach(endpointEntity -> endpointEntity.setOrganization(entity));
        entity.setEndpoints(endpoints);

        try {
            final OrganizationEntity persistedOrg = this.dao.registerOrganization(entity);
            return Response.status(Response.Status.CREATED).entity(this.converter.toFHIR(Organization.class, persistedOrg)).build();
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw e;
        }
    }

    @GET
    @Path("/{organizationID}")
    @FHIR
    @UnitOfWork
    @Override
    public Organization getOrganization(
            @PathParam("organizationID") UUID organizationID) {
        final Optional<OrganizationEntity> orgOptional = this.dao.fetchOrganization(organizationID);
        final OrganizationEntity organizationEntity = orgOptional.orElseThrow(() -> new WebApplicationException(String.format("Cannot find organization '%s'", organizationID), Response.Status.NOT_FOUND));
        return this.converter.toFHIR(Organization.class, organizationEntity);
    }

    @PUT
    @Path("/{organizationID}")
    @FHIR
    @UnitOfWork
    @Override
    public Response updateOrganization(@PathParam("organizationID") UUID organizationID, Organization organization) {
        try {
            OrganizationEntity orgEntity = this.converter.fromFHIR(OrganizationEntity.class, organization);
            Organization original = getOrganization(organizationID);
            List<EndpointEntity> endpointEntities = original.getEndpoint().stream().map(
                    r -> {
                        UUID endpointID = FHIRExtractors.getEntityUUID(r.getReference());
                        return endpointDAO.fetchEndpoint(endpointID);
                    }
            ).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
            orgEntity.setEndpoints(endpointEntities);
            orgEntity = this.dao.updateOrganization(organizationID, orgEntity);
            return Response.status(Response.Status.OK).entity(this.converter.toFHIR(Organization.class, orgEntity)).build();
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw e;
        }
    }

    @DELETE
    @Path("/{organizationID}")
    @FHIR
    @UnitOfWork
    @Override
    public Response deleteOrganization(@PathParam("organizationID") UUID organizationID) {
        final OrganizationEntity organizationEntity = this.dao.fetchOrganization(organizationID)
                .orElseThrow(() -> new WebApplicationException("Cannot find organization.", Response.Status.NOT_FOUND));

        this.dao.deleteOrganization(organizationEntity);
        return Response.ok().build();
    }

    private List<EndpointEntity> extractEndpoints(Bundle transactionBundle) {
        return transactionBundle
                .getEntry()
                .stream()
                .filter(entry -> entry.hasResource() && entry.getResource().getResourceType().getPath().equals(DPCResourceType.Endpoint.getPath()))
                .map(entry -> (Endpoint) entry.getResource())
                .map(e -> this.converter.fromFHIR(EndpointEntity.class, e))
                .collect(Collectors.toList());
    }

    private String generateNewOrgId() {
        final List<String> prohibitedIds = config.getLookBackExemptOrgs();
        String orgId;
        do {
            orgId = uuidSupplier.get().toString();
        } while (prohibitedIds != null && prohibitedIds.contains(orgId));
        return orgId;
    }
}
