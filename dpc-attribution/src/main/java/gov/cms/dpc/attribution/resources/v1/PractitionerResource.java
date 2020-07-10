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
import io.swagger.annotations.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static gov.cms.dpc.attribution.utils.RESTUtils.bulkResourceHandler;

@FHIR
@Api(value = "Practitioner")
public class PractitionerResource extends AbstractPractitionerResource {

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
    @ApiOperation(value = "Search for providers", notes = "FHIR endpoint to search for Practitioner resources." +
            "<p>If a provider NPI is given, the results are filtered accordingly. " +
            "Otherwise, the method returns all Practitioners associated to the given Organization." +
            "<p> It's possible to provide a specific resource ID and Organization ID, for use in Authorization.", response = Bundle.class)
    public List<Practitioner> getPractitioners(@ApiParam(value = "Practitioner resource ID")
                                               @QueryParam("_id") UUID resourceID,
                                               @ApiParam(value = "Provider NPI")
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
    @ApiOperation(value = "Register provider", notes = "FHIR endpoint to register a provider with the system." +
            "<p>Each provider must have a metadata Tag with the responsible Organization ID included." +
            "If not, we'll reject it." +
            "<p> If a provider is already registered with the Organization, an error is thrown." +
            "<p> If an organization has already reached the provider limit, then an error is thrown")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "New resource was created"),
            @ApiResponse(code = 422, message = "Unprocessable resource")
    })
    public Response submitProvider(Practitioner provider) {

        final ProviderEntity entity = this.converter.fromFHIR(ProviderEntity.class, provider);
        final Long totalExistingProviders = this.dao.getProvidersCount(null, null, entity.getOrganization().getId());
        final List<ProviderEntity> existingProvidersByNPI = this.dao.getProviders(null, entity.getProviderNPI(), entity.getOrganization().getId());

        if (providerLimit != null && providerLimit != -1 && totalExistingProviders >= providerLimit) {
            return Response.status(422).entity(this.converter.toFHIR(Practitioner.class, existingProvidersByNPI.get(0))).build();
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
    @ApiOperation(value = "Fetch provider", notes = "FHIR endpoint to fetch a specific Practitioner resource." +
            "<p>Note: FHIR refers to *Providers* as *Practitioners* and names the resources and endpoints accordingly")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No matching Practitioner resource was found", response = OperationOutcome.class)
    })
    public Practitioner getProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID) {
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
    @ApiOperation(value = "Bulk submit Practitioner resources", notes = "FHIR operation for submitting a Bundle of Practitioner resources, which will be associated to the given Organization.", response = Bundle.class)
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
    @ApiOperation(value = "Delete provider", notes = "FHIR endpoint to remove the given Practitioner resource")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No matching Practitioner resource was found", response = OperationOutcome.class)
    })
    public Response deleteProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID) {
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
    @ApiOperation(value = "Update provider", notes = "FHIR endpoint to update the given Practitioner resource with new values.")
    @ApiResponses(@ApiResponse(code = 404, message = "Cannot find Practitioner"))
    public Practitioner updateProvider(@ApiParam(value = "Practitioner resource ID", required = true) @PathParam("providerID") UUID providerID, Practitioner provider) {
        final ProviderEntity providerEntity = this.converter.fromFHIR(ProviderEntity.class, provider);
        providerEntity.setID(providerID);
        return this.converter.toFHIR(Practitioner.class, this.dao.updateProvider(providerID, providerEntity));
    }
}
