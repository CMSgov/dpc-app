package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.resources.AbstractPractionerResource;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.hibernate.UnitOfWork;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.UUID;


@FHIR
public class PractitionerResource extends AbstractPractionerResource {

    private final ProviderDAO dao;

    @Inject
    PractitionerResource(ProviderDAO dao) {
        this.dao = dao;
    }

    @GET
    @UnitOfWork
    @Override
    // TODO: Migrate this signature to a List<Practitioner> in DPC-302
    public Bundle getPractitioners(@QueryParam("identifier") String providerNPI) {
        final Bundle bundle = new Bundle();
        this.dao.getProviders(providerNPI)
                .stream()
                .map(ProviderEntity::toFHIR)
                .forEach(provider -> bundle.addEntry().setResource(provider));

        return bundle;
    }

    @POST
    @FHIR
    @UnitOfWork
    @Override
    public Practitioner submitProvider(Practitioner provider) {

        final ProviderEntity entity = ProviderEntity.fromFHIR(provider);
        final ProviderEntity persistedEntity = this.dao.persistProvider(entity);

        return persistedEntity.toFHIR();
    }

    @GET
    @Path("/{providerID}")
    @UnitOfWork
    @Override
    public Practitioner getProvider(@PathParam("providerID") UUID providerID) {
        final ProviderEntity providerEntity = this.dao
                .getProvider(providerID)
                .orElseThrow(() ->
                        new WebApplicationException(String.format("Provider %s is not registered",
                                providerID), Response.Status.NOT_FOUND));

        return providerEntity.toFHIR();
    }

    @DELETE
    @Path("/{providerID}")
    @UnitOfWork
    @Override
    public Response deleteProvider(@PathParam("providerID") UUID providerID) {
        try {
            this.dao.deleteProvider(providerID);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(String.format("Provider '%s' is not registered", providerID), Response.Status.NOT_FOUND);
        }

        return Response.ok().build();
    }
}
