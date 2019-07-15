package gov.cms.dpc.attribution.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.resources.AbstractPractionerResource;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.hibernate.UnitOfWork;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
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
    @Timed
    @ExceptionMetered
    // TODO: Migrate this signature to a List<Practitioner> in DPC-302
    public Bundle getPractitioners(@QueryParam("identifier") String providerNPI, @QueryParam("_tag") String organizationTag) {
        final Bundle bundle = new Bundle();
        final List<ProviderEntity> providers = this.dao.getProviders(providerNPI, splitTag(organizationTag));

        bundle.setTotal(providers.size());
        providers.forEach(provider -> bundle.addEntry().setResource(provider.toFHIR()));

        return bundle;
    }

    @POST
    @FHIR
    @UnitOfWork
    @Override
    @Timed
    @ExceptionMetered
    public Response submitProvider(Practitioner provider) {

        final ProviderEntity entity = ProviderEntity.fromFHIR(provider);
        final ProviderEntity persistedEntity = this.dao.persistProvider(entity);

        return Response.status(Response.Status.CREATED).entity(persistedEntity.toFHIR()).build();
    }

    @GET
    @Path("/{providerID}")
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

        return providerEntity.toFHIR();
    }

    @DELETE
    @Path("/{providerID}")
    @UnitOfWork
    @Override
    @Timed
    @ExceptionMetered
    public Response deleteProvider(@PathParam("providerID") UUID providerID) {
        try {
            this.dao.deleteProvider(providerID);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(String.format("Provider '%s' is not registered", providerID), Response.Status.NOT_FOUND);
        }

        return Response.ok().build();
    }

    @PUT
    @Path("/{providerID}")
    @UnitOfWork
    @Override
    @Timed
    @ExceptionMetered
    public Practitioner updateProvider(@PathParam("providerID") UUID providerID, Practitioner provider) {
        final ProviderEntity providerEntity = this.dao.persistProvider(ProviderEntity.fromFHIR(provider, providerID));

        return providerEntity.toFHIR();
    }

    private static UUID splitTag(String tag) {
        final String[] split = tag.split("\\|", -1);

        if (split.length < 2) {
            throw new IllegalArgumentException("Must have | delimiter in tag");
        }

        return FHIRExtractors.getEntityUUID(split[1]);
    }
}
