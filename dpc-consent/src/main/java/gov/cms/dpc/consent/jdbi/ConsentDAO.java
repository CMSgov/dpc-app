package gov.cms.dpc.consent.jdbi;

import gov.cms.dpc.consent.entities.ConsentEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
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

    public List<ConsentEntity> list() {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<ConsentEntity> query = builder.createQuery(ConsentEntity.class);
        query.from(ConsentEntity.class);
        return list(query);
    }
}
