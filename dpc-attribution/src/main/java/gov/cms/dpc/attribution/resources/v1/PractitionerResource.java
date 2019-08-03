package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.resources.AbstractPractitionerResource;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.*;
import org.eclipse.jetty.http.HttpStatus;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@FHIR
@Api(value = "Practitioner")
public class PractitionerResource extends AbstractPractitionerResource {

    private final ProviderDAO dao;

    @Inject
    PractitionerResource(ProviderDAO dao) {
        this.dao = dao;
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
            "<p> It's possible to provide a specific resource ID and Organization ID, for use in Authorization.")
    // TODO: Migrate this signature to a List<Practitioner> in DPC-302
    public Bundle getPractitioners(@ApiParam(value = "Practitioner resource ID")
                                   @QueryParam("_id") UUID resourceID,
                                   @ApiParam(value = "Provider NPI")
                                   @QueryParam("identifier") String providerNPI,
                                   @NotEmpty @QueryParam("organization") String organizationID) {

        final Bundle bundle = new Bundle();
        final List<ProviderEntity> providers = this.dao.getProviders(resourceID, providerNPI, FHIRExtractors.getEntityUUID(organizationID));

        bundle.setTotal(providers.size());
        bundle.setType(Bundle.BundleType.SEARCHSET);
        providers.forEach(provider -> bundle.addEntry().setResource(provider.toFHIR()));
        return bundle;
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
            "<p> If a provider is already registered with the Organization, an errorr is thrown.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "New resource was created"),
    })
    public Response submitProvider(Practitioner provider) {

        final ProviderEntity entity = ProviderEntity.fromFHIR(provider);
        final ProviderEntity persisted = this.dao.persistProvider(entity);
        // If a new record exists, return it with the created status
        return Response.status(Response.Status.CREATED).entity(persisted.toFHIR()).build();
    }

    @POST
    @Path("/$submit")
    @FHIR
    @UnitOfWork
    @Timed
    @ExceptionMetered
    @Override
    public Bundle bulkSubmitProviders(Parameters params) {
        final Bundle providerBundle = (Bundle) params.getParameterFirstRep().getResource();
        final Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        // Grab all of the providers and submit them individually (for now)
        // TODO: Optimize insert as part of DPC-490

        final List<BundleEntryComponent> bundleEntries = providerBundle
                .getEntry()
                .stream()
                .filter(BundleEntryComponent::hasResource)
                .map(BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType() == ResourceType.Practitioner)
                .map(resource -> (Practitioner) resource)
                .map(provider -> {
                    final Response response = this.submitProvider(provider);
                    if (HttpStatus.isSuccess(response.getStatus())) {
                        return (Practitioner) response.getEntity();
                    }
                    // If there's an error, rethrow the original method
                    throw new WebApplicationException(response);
                })
                .map(practitoner -> new BundleEntryComponent().setResource(practitoner))
                .collect(Collectors.toList());

        bundle.setEntry(bundleEntries);
        bundle.setTotal(bundleEntries.size());
        return bundle;
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

        return providerEntity.toFHIR();
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
        final ProviderEntity providerEntity = this.dao.persistProvider(ProviderEntity.fromFHIR(provider, providerID));
        return providerEntity.toFHIR();
    }
}
