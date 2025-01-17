package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.resources.AbstractPractitionerResource;
import gov.cms.dpc.attribution.utils.RESTUtils;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.BundleReturnProperties;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import io.dropwizard.hibernate.UnitOfWork;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FHIR
public class PractitionerResource extends AbstractPractitionerResource {

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

    @GET
    @FHIR
    @UnitOfWork
    @Override
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

    @POST
    @FHIR
    @UnitOfWork
    @Override
    @Timed
    @ExceptionMetered
    public Response submitProvider(Practitioner provider) {
        final ProviderEntity entity = this.converter.fromFHIR(ProviderEntity.class, provider);
        final Long totalExistingProviders = this.dao.getProvidersCount(null, null, entity.getOrganization().getId());
        final List<ProviderEntity> existingProvidersByNPI = this.dao.getProviders(null, entity.getProviderNPI(), entity.getOrganization().getId());

        if (providerLimit != null && providerLimit != -1 && totalExistingProviders >= providerLimit) {
            return Response.status(422).entity(this.converter.toFHIR(Practitioner.class, entity)).build();
        }

        if (existingProvidersByNPI.isEmpty()) {
            final Practitioner persisted = insertPractitioner(provider);
            return Response.status(Response.Status.CREATED).entity(persisted).build();
        }

        return Response.ok().entity(this.converter.toFHIR(Practitioner.class, existingProvidersByNPI.get(0))).build();
    }

    @GET
    @Path("/{providerID}")
    @FHIR
    @UnitOfWork
    @Override
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

    @POST
    @Path("/$submit")
    @FHIR
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @BundleReturnProperties(bundleType = Bundle.BundleType.COLLECTION)
    @Override
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

        // Insert the practitioners that don't already exist
        List<Practitioner> insertedPractitioners = RESTUtils.bulkResourceHandler(
            FHIRExtractors.getResourceStream(params, Practitioner.class), this::insertPractitioner, dao, dbBatchSize
        );

        return Stream.concat(
            insertedPractitioners.stream(),
            existingProviderEntities.stream().map(entity -> this.converter.toFHIR(Practitioner.class, entity))
        ).collect(Collectors.toList());
    }

    @DELETE
    @Path("/{providerID}")
    @FHIR
    @UnitOfWork
    @Override
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

    @PUT
    @Path("/{providerID}")
    @FHIR
    @UnitOfWork
    @Override
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
