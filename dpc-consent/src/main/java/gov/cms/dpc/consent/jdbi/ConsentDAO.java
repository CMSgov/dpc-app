package gov.cms.dpc.consent.jdbi;

import gov.cms.dpc.common.entities.ConsentEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConsentDAO extends AbstractDAO<ConsentEntity> {

    @Inject
    public ConsentDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public ConsentEntity persistConsent(ConsentEntity consent) {
        return this.persist(consent);
    }

    public Optional<ConsentEntity> getConsent(UUID consentID) {
        return Optional.ofNullable(get(consentID));
    }

    public List<ConsentEntity> getConsentsByHICN(String hicn) {
        EntityManager entityManager = currentSession().getEntityManagerFactory().createEntityManager();
        return entityManager
                .createQuery("select c from ConsentEntity c where c.hicn = :hicn", ConsentEntity.class)
                .setParameter("hicn", hicn)
                .getResultList();
    }
}
