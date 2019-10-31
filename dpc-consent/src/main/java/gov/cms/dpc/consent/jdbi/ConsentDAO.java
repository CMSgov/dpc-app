package gov.cms.dpc.consent.jdbi;

import gov.cms.dpc.common.hibernate.consent.DPCConsentManagedSessionFactory;
import gov.cms.dpc.consent.entities.ConsentEntity;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConsentDAO extends AbstractDAO<ConsentEntity> {

    @Inject
    public ConsentDAO(DPCConsentManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public ConsentEntity persistConsent(ConsentEntity consentEntity) {
        return this.persist(consentEntity);
    }

    public Optional<ConsentEntity> getConsent(UUID consentID) {
        return Optional.ofNullable(get(consentID));
    }

    public List<ConsentEntity> list() {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<ConsentEntity> query = builder.createQuery(ConsentEntity.class);
        query.from(ConsentEntity.class);
        return list(query);
    }
}
