package gov.cms.dpc.attribution.resources.r4;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.jdbi.r4.ProviderDAO;
import gov.cms.dpc.attribution.utils.r4.RESTUtils;
import gov.cms.dpc.common.entities.r4.ProviderEntity;
import gov.cms.dpc.fhir.r4.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.r4.BundleReturnProperties;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.common.hibernate.attribution.DPCR4HibernateBundle;
import io.dropwizard.hibernate.UnitOfWork;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FHIR
public class PractitionerResource {

    private static final Logger logger = LoggerFactory.getLogger(PractitionerResource.class);

    private final ProviderDAO dao;
    private final FHIREntityConverter converter;
    private final Integer providerLimit;
    private final int dbBatchSize;

    @Inject
    PractitionerResource(FHIREntityConverter converter, ProviderDAO dao, DPCAttributionConfiguration dpcAttributionConfiguration) {
        this.dao = dao;
        this.converter = converter;
        this.providerLimit = dpcAttributionConfiguration.getProviderLimit();
        this.dbBatchSize = dpcAttributionConfiguration.getDbBatchSize();
    }

    /**
     * FHIR search endpoint which allows querying providers with a given NPI
     * <p>
     * An Organization ID is required for this endpoint, which is passed via the `_tag` query parameter.
     *
     * @param resourceID      - {@link UUID} resource ID to query for
     * @param providerNPI     - {@link String} NPI to use for querying Provider database
     * @param organizationID - {@link String} ID of {@link org.hl7.fhir.r4.model.Organization} making the request
     * @return - {@link Bundle} of {@link Practitioner} resources matching search parameters
     */
    @GET
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    @Timed
    @ExceptionMetered
    public List<Practitioner> getPractitioners(@QueryParam("_id") UUID resourceID,
                                               @QueryParam("identifier") String providerNPI,
                                               @NotEmpty @QueryParam("organization") String organizationID) {
        return this.dao
                .getProviders(resourceID, providerNPI, FHIRExtractors.getEntityUUID(organizationID))
                .stream()
                .map(p -> this.converter.toFHIR(Practitioner.class, p))
                .collect(Collectors.toList());
    }

    /**
     * Register {@link Practitioner} with application.
     * <p>
     * Note: No {@link org.hl7.fhir.r4.model.PractitionerRole} is created by this endpoint, so the {@link Practitioner} is registered with the system, but not assigned to an {@link org.hl7.fhir.r4.model.Organization}
     *
     * @param provider = {@link Practitioner}
     * @return - {@link Practitioner} with additional metadata added by application
     */
    @POST
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    @Timed
    @ExceptionMetered
    public Response submitProvider(Practitioner provider) {
        final ProviderEntity entity = this.converter.fromFHIR(ProviderEntity.class, provider);
        final Long totalExistingProviders = this.dao.getProvidersCount(null, null, entity.getOrganization().getId());
        final List<ProviderEntity> existingProvidersByNPI = this.dao.getProviders(null, entity.getProviderNPI(), entity.getOrganization().getId());

        if (providerLimit != null && providerLimit != -1 && totalExistingProviders >= providerLimit) {
            logger.warn("Provider limit reached for submitProvider() operation: providerLimit={} orgId={} currentCount={} providerNPI={}",
                    providerLimit,
                    entity.getOrganization().getId(),
                    totalExistingProviders,
                    entity.getProviderNPI());
            OperationOutcome outcomeWithProviderLimitMessage = new OperationOutcome();
            outcomeWithProviderLimitMessage.addIssue()
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setCode(OperationOutcome.IssueType.BUSINESSRULE)
                    .setDiagnostics("Provider limit reached");

            return Response.status(422)
                    .entity(outcomeWithProviderLimitMessage)
                    .type("application/fhir+json")
                    .build();
        }

        if (existingProvidersByNPI.isEmpty()) {
            final Practitioner persisted = insertPractitioner(provider);
            return Response.status(Response.Status.CREATED).entity(persisted).build();
        }

        return Response.ok().entity(this.converter.toFHIR(Practitioner.class, existingProvidersByNPI.get(0))).build();
    }

    /**
     * Fetch specific {@link Practitioner} resource
     * This is the ID of the resource, NOT the identifying NPI
     *
     * @param providerID - {@link UUID} provider ID
     * @return - {@link Practitioner}
     */
    @GET
    @Path("/{providerID}")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    @Timed
    @ExceptionMetered
    public Practitioner getProvider(@PathParam("providerID") UUID providerID) {
        final ProviderEntity providerEntity = this.dao
                .getProvider(providerID)
                .orElseThrow(() ->
                        new WebApplicationException(String.format("Provider %s is not registered",
                                providerID), Response.Status.NOT_FOUND));

        return this.converter.toFHIR(Practitioner.class, providerEntity);
    }

    /**
     * Submit a {@link Bundle} of {@link Practitioner} resources as a single transaction
     * Internally, this is implemented by calling {@link PractitionerResource#submitProvider(Practitioner)} for each entry in the bundle.
     * This will be optimized at a later date
     *
     * @param params - {@link Bundle} of {@link Practitioner} resources to submit
     * @return - {@link Bundle} of newly created {@link Practitioner} resources
     */
    @POST
    @Path("/$submit")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    @Timed
    @ExceptionMetered
    @BundleReturnProperties(bundleType = Bundle.BundleType.COLLECTION)
    public List<Practitioner> bulkSubmitProviders(Parameters params) {
        // Get the org from one of the practitioners
        Optional<Practitioner> firstPractitioner = FHIRExtractors.getResourceStream(params, Practitioner.class).findAny();
        if (firstPractitioner.isEmpty()) {
            throw new WebApplicationException("No practitioners submitted", HttpStatus.UNPROCESSABLE_ENTITY_422);
        }
        final UUID orgId = FHIRExtractors.getEntityUUID(FHIRExtractors.getOrganizationID(firstPractitioner.get()));

        // Get NPIs of practitioners that already exist in the DB
        final List<String> npis = FHIRExtractors.getResourceStream(params, Practitioner.class)
            .map(FHIRExtractors::getProviderNPI)
            .collect(Collectors.toList());

        // Get practitioners that already exist in the DB
        final List<ProviderEntity> existingProviderEntities = dao.bulkProviderSearch(orgId, npis);

        // Extract NPIs of practitioners that already exist in the DB
        final List<String> existingNpis = existingProviderEntities.stream().map(ProviderEntity::getProviderNPI).collect(Collectors.toList());

        // Insert the practitioners that don't already exist
        List<Practitioner> insertedPractitioners = RESTUtils.bulkResourceHandler(
            FHIRExtractors.getResourceStream(params, Practitioner.class)
                .filter(practitioner -> ! existingNpis.contains(FHIRExtractors.getProviderNPI(practitioner))),
            this::insertPractitioner,
            dao,
            dbBatchSize
        );

        return Stream.concat(
            insertedPractitioners.stream(),
            existingProviderEntities.stream().map(entity -> this.converter.toFHIR(Practitioner.class, entity))
        ).collect(Collectors.toList());
    }

    /**
     * Remove {@link Practitioner} resource
     * This also removes all depending resource (e.g. attribution lists)
     *
     * @param providerID - {@link UUID} provider ID
     * @return - {@link Response}
     */
    @DELETE
    @Path("/{providerID}")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    @Timed
    @ExceptionMetered
    public Response deleteProvider(@PathParam("providerID") UUID providerID) {
        try {
            final ProviderEntity provider = this.dao.getProvider(providerID).orElseThrow(() -> new WebApplicationException(String.format("Provider '%s' is not registered", providerID), Response.Status.NOT_FOUND));
            this.dao.deleteProvider(provider);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(String.format("Provider '%s' is not registered", providerID), Response.Status.NOT_FOUND);
        }
    }

    /**
     * Update {@link Practitioner} resource
     *
     * @param providerID - {@link UUID} provider ID
     * @param provider   - {@link Practitioner} resource to update
     * @return - {@link Practitioner} newly updated resource
     */
    @PUT
    @Path("/{providerID}")
    @FHIR
    @UnitOfWork(DPCR4HibernateBundle.BUNDLE_NAME)
    @Timed
    @ExceptionMetered
    public Practitioner updateProvider(@PathParam("providerID") UUID providerID, Practitioner provider) {
        final ProviderEntity providerEntity = this.converter.fromFHIR(ProviderEntity.class, provider);
        providerEntity.setID(providerID);
        return this.converter.toFHIR(Practitioner.class, this.dao.updateProvider(providerID, providerEntity));
    }

    private Practitioner insertPractitioner(Practitioner practitioner) {
        ProviderEntity providerEntity = this.converter.fromFHIR(ProviderEntity.class, practitioner);
        providerEntity.setID(null);
        return this.converter.toFHIR(Practitioner.class, this.dao.persistProvider(providerEntity));
    }
}
