package gov.cms.dpc.attribution.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.dpc.attribution.jdbi.ProviderDAO;
import gov.cms.dpc.attribution.resources.AbstractPractionerResource;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.hibernate.UnitOfWork;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Practitioner;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@FHIR
public class PractitionerResource extends AbstractPractionerResource {

    private final ProviderDAO dao;

    @Inject
    PractitionerResource(ProviderDAO dao) {
        this.dao = dao;
    }

    @UnitOfWork
    @Override
    public List<Practitioner> getPractitioners(@QueryParam("id") String providerNPI) {
        return this.dao.getProviders(providerNPI)
                .stream()
                .map(ProviderEntity::toFHIR)
                .collect(Collectors.toList());
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
}
