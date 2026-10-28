package gov.cms.dpc.attribution.resources.r4;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.jdbi.r4.OrganizationDAO;
import gov.cms.dpc.common.entities.r4.OrganizationEntity;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.r4.FHIRParameter;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.common.hibernate.attribution.DPCR4HibernateBundle;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static gov.cms.dpc.attribution.utils.r4.RESTUtils.parseTokenTag;

public class OrganizationResource {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationResource.class);

    private final OrganizationDAO dao;
    private final FHIREntityConverter converter;
    private final DPCAttributionConfiguration config;
    private final Supplier<UUID> uuidSupplier = UUID::randomUUID;

    @Inject
    OrganizationResource(FHIREntityConverter converter, OrganizationDAO dao, DPCAttributionConfiguration config) {
        this.converter = converter;
        this.dao = dao;
        this.config = config;
    }

    @GET
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public List<Organization> searchOrganizations(
            @QueryParam("identifier") String identifier) {

        if (identifier == null) {
            return this.dao.listOrganizations()
                    .stream()
                    .map(o -> this.converter.toFHIR(Organization.class, o))
                    .collect(Collectors.toList());
        }
        String parsedToken = parseTokenTag(tag -> tag, identifier);
        Set<String> idSet = Set.of(parsedToken.split(","));
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

    /**
     * Register a {@link Organization} with the API
     * <p>
     * We're currently using a {@link Bundle} resource, which allows us to include the {@link Organization}.
     * <p>
     * The {@link Bundle} is submitted as the Resource portion of the {@link Parameters} object.
     *
     * @param transactionBundle - {@link Bundle} to register with system, submitted via a {@link Parameters} object.
     * @return - {@link Response} whether operation succeeded or failed
     */
    @POST
    @Path("/$submit")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
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
        try {
            final OrganizationEntity persistedOrg = this.dao.registerOrganization(entity);
            return Response.status(Response.Status.CREATED).entity(this.converter.toFHIR(Organization.class, persistedOrg)).build();
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw e;
        }
    }

    /**
     * Fetch the {@link Organization} with the given ID
     *
     * @param organizationID - {@link UUID} of organization
     * @return - {@link Organization}
     */
    @GET
    @Path("/{organizationID}")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public Organization getOrganization(
            @PathParam("organizationID") UUID organizationID) {
        final Optional<OrganizationEntity> orgOptional = this.dao.fetchOrganization(organizationID);
        final OrganizationEntity organizationEntity = orgOptional.orElseThrow(() -> new WebApplicationException(String.format("Cannot find organization '%s'", organizationID), Response.Status.NOT_FOUND));
        return this.converter.toFHIR(Organization.class, organizationEntity);
    }

    /**
     * Update the {@link Organization} with the given ID
     *
     * @param organizationID {@link UUID} of organization
     * @param organization   {@link Organization}
     * @return - {@link Response} whether the {@link Organization} was updated
     */
    @PUT
    @Path("/{organizationID}")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public Response updateOrganization(@PathParam("organizationID") UUID organizationID, Organization organization) {
        try {
            OrganizationEntity orgEntity = this.converter.fromFHIR(OrganizationEntity.class, organization);
            orgEntity = this.dao.updateOrganization(organizationID, orgEntity);
            return Response.status(Response.Status.OK).entity(this.converter.toFHIR(Organization.class, orgEntity)).build();
        } catch (Exception e) {
            logger.error("Error: ", e);
            throw e;
        }
    }

    /**
     * Delete the {@link Organization} from the system.
     * <p>
     * Note: This drop ALL resources associated to the given Organization
     *
     * @param organizationID - {@link UUID} of organization
     * @return - {@link Response} whether the drop was successful
     */
    @DELETE
    @Path("/{organizationID}")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    public Response deleteOrganization(@PathParam("organizationID") UUID organizationID) {
        final OrganizationEntity organizationEntity = this.dao.fetchOrganization(organizationID)
                .orElseThrow(() -> new WebApplicationException("Cannot find organization.", Response.Status.NOT_FOUND));

        this.dao.deleteOrganization(organizationEntity);
        return Response.ok().build();
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
