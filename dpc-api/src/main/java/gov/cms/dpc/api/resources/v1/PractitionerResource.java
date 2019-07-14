package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.resources.AbstractPractionerResource;
import io.dropwizard.auth.Auth;
import org.hl7.fhir.dstu3.model.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class PractitionerResource extends AbstractPractionerResource {

    private final IGenericClient client;

    @Inject
    PractitionerResource(IGenericClient client) {
        this.client = client;
    }

    @Override
    @GET
    @Timed
    @ExceptionMetered
    public Bundle getPractitioners(@Auth OrganizationPrincipal organization, String providerNPI) {
        return this.client
                .search()
                .forResource(Practitioner.class)
                .encodedJson()
                .where(Patient.IDENTIFIER.exactly().identifier(providerNPI))
                .and(Organization.RES_ID.exactly().identifier(organization.getOrganization().getId()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
    }

    @Override
    @GET
    @Path("/{providerID}")
    @Timed
    @ExceptionMetered
    public Practitioner getProvider(@PathParam("providerID") UUID providerID) {
        return this.client
                .read()
                .resource(Practitioner.class)
                .withId(providerID.toString())
                .encodedJson()
                .execute();
    }

    @Override
    @POST
    @Timed
    @ExceptionMetered
    public Practitioner submitProvider(@Auth OrganizationPrincipal organization, Practitioner provider) {
         final var test= this.client
                .create()
                .resource(provider)
                .encodedJson();

        final MethodOutcome outcome = test.execute();

        final Practitioner resource = (Practitioner) outcome.getResource();
        if (resource == null) {
            throw new WebApplicationException("Unable to submit provider", Response.Status.INTERNAL_SERVER_ERROR);
        }

        // Now, submit the Practitioner Role
        final PractitionerRole role = new PractitionerRole();
        role.setOrganization(new Reference(organization.getOrganization().getIdElement()));
        role.setPractitioner(new Reference(resource.getIdElement()));

        final MethodOutcome roled = this.client
                .create()
                .resource(role)
                .encodedJson()
                .execute();

        if (!roled.getCreated()) {
            throw new WebApplicationException("Unable to link provider to organization", Response.Status.INTERNAL_SERVER_ERROR);
        }
        return resource;
    }

    @Override
    @DELETE
    @Path("/{providerID}")
    @Timed
    @ExceptionMetered
    public Response deleteProvider(@PathParam("providerID") UUID providerID) {
        this.client
                .delete()
                .resourceById(new IdType("Practitioner", providerID.toString()))
                .encodedJson()
                .execute();

        return Response.ok().build();
    }

    @Override
    @Timed
    @PUT
    @Path("/{providerID}")
    @ExceptionMetered
    public Practitioner updateProvider(@PathParam("providerID") UUID providerID, Practitioner provider) {
        return null;
    }
}
