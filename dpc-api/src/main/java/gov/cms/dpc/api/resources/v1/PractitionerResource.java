package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.name.Named;
import gov.cms.dpc.api.APIHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.Authorizer;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.resources.AbstractPractitionerResource;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.annotations.FHIR;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.ValidationHelpers;
import gov.cms.dpc.fhir.validations.profiles.PractitionerProfile;
import io.dropwizard.auth.Auth;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.Consumer;

import static gov.cms.dpc.api.APIHelpers.bulkResourceClient;
import static gov.cms.dpc.fhir.helpers.FHIRHelpers.handleMethodOutcome;

@Path("/v1/Practitioner")
public class PractitionerResource extends AbstractPractitionerResource {

    private static final String PRACTITIONER_PROFILE = "https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-practitioner";
    private static final Logger logger = LoggerFactory.getLogger(PractitionerResource.class);
    private final IGenericClient client;
    private final FhirValidator validator;

    @Inject
    PractitionerResource(@Named("attribution") IGenericClient client, FhirValidator validator) {
        this.client = client;
        this.validator = validator;
    }

    @GET
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @Override
    public Bundle practitionerSearch(@Auth OrganizationPrincipal organization, @QueryParam(value = Practitioner.SP_IDENTIFIER) @NoHtml String providerNPI) {

        // Create search params
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("organization", Collections
                .singletonList(organization
                        .getOrganization()
                        .getIdElement()
                        .getIdPart()));

        final var request = this.client
                .search()
                .forResource(Practitioner.class)
                .encodedJson()
                .returnBundle(Bundle.class);

        if (providerNPI != null && !providerNPI.equals("")) {
            searchParams.put("identifier", Collections.singletonList(providerNPI));
        }

        return request
                .whereMap(searchParams)
                .execute();
    }

    @GET
    @FHIR
    @Path("/{providerID}")
    @PathAuthorizer(type = DPCResourceType.Practitioner, pathParam = "providerID")
    @Timed
    @ExceptionMetered
    @Override
    public Practitioner getProvider(@PathParam("providerID") UUID providerID) {
        return this.client
                .read()
                .resource(Practitioner.class)
                .withId(providerID.toString())
                .encodedJson()
                .execute();
    }

    @POST
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @Override
    public Response submitProvider(@Auth OrganizationPrincipal organizationPrincipal, @Valid @Profiled Practitioner provider) {

        APIHelpers.addOrganizationTag(provider, organizationPrincipal.getID().toString());
        final var providerCreate = this.client
                .create()
                .resource(provider)
                .encodedJson();

        return handleMethodOutcome(providerCreate.execute());
    }

    @POST
    @Path("/$submit")
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @Override
    public Bundle bulkSubmitProviders(@Auth OrganizationPrincipal organization, Parameters params) {
        final Bundle providerBundle = (Bundle) params.getParameterFirstRep().getResource();
        final Consumer<Practitioner> entryHandler = (resource) -> validateProvider(resource,
                organization.getOrganization().getId(),
                validator,
                PRACTITIONER_PROFILE);

        return bulkResourceClient(Practitioner.class, client, entryHandler, providerBundle);
    }

    @DELETE
    @Path("/{providerID}")
    @PathAuthorizer(type = DPCResourceType.Practitioner, pathParam = "providerID")
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    public Response deleteProvider(@PathParam("providerID") UUID providerID) {
        this.client
                .delete()
                .resourceById(new IdType("Practitioner", providerID.toString()))
                .encodedJson()
                .execute();

        return Response.ok().build();
    }

    @PUT
    @Path("/{providerID}")
    @PathAuthorizer(type = DPCResourceType.Practitioner, pathParam = "providerID")
    @FHIR
    @Timed
    @ExceptionMetered
    @Override
    public Practitioner updateProvider(@PathParam("providerID") UUID providerID, @Valid @Profiled Practitioner provider) {
        throw new WebApplicationException("Update Practitioner not yet implemented.", Response.Status.NOT_IMPLEMENTED);
    }

    @POST
    @Path("/$validate")
    @FHIR
    @Timed
    @ExceptionMetered
    @Authorizer
    @Override
    public IBaseOperationOutcome validateProvider(@Auth OrganizationPrincipal organization, Parameters parameters) {
        return ValidationHelpers.validateAgainstProfile(this.validator, parameters, PractitionerProfile.PROFILE_URI);
    }

    private static void validateProvider(Practitioner provider, String organizationID, FhirValidator validator, String profileURL) {
        logger.debug("Validating Practitioner {}", provider.toString());
        final ValidationResult result = validator.validateWithResult(provider, new ValidationOptions().addProfile(profileURL));
        if (!result.isSuccessful()) {
            throw new WebApplicationException(APIHelpers.formatValidationMessages(result.getMessages()), HttpStatus.UNPROCESSABLE_ENTITY_422);
        }
        APIHelpers.addOrganizationTag(provider, organizationID);
    }

}
