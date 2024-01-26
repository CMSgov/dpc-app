package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.resources.AbstractPractitionerResource;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.BundleReturnProperties;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import io.dropwizard.hibernate.UnitOfWork;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static gov.cms.dpc.attribution.utils.RESTUtils.bulkResourceHandler;

@FHIR
public class PractitionerResource extends AbstractPractitionerResource {
    private static final Logger logger = LoggerFactory.getLogger(PractitionerResource.class);

    private final ProviderDAO dao;
    private final FHIREntityConverter converter;
    private final Integer providerLimit;

    @Inject
    PractitionerResource(FHIREntityConverter converter, ProviderDAO dao, DPCAttributionConfiguration dpcAttributionConfiguration) {
        this.dao = dao;
        this.converter = converter;
        this.providerLimit = dpcAttributionConfiguration.getProviderLimit();
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
            final ProviderEntity persisted = this.dao.persistProvider(entity);
            return Response.status(Response.Status.CREATED).entity(this.converter.toFHIR(Practitioner.class, persisted)).build();
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
        return bulkResourceHandler(Practitioner.class, params, this::submitProvider);
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
}
