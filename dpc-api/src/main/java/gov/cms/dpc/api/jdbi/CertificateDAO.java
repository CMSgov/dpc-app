package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.common.entities.CertificateEntity;
import gov.cms.dpc.common.entities.CertificateEntity_;
import gov.cms.dpc.common.entities.OrganizationEntity_;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

public class CertificateDAO extends AbstractDAO<CertificateEntity> {

    @Inject
    CertificateDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public CertificateEntity persistCertificate(CertificateEntity entity) {
        return persist(entity);
    }

    public List<CertificateEntity> fetchCertificate(UUID certificateID, UUID organizationID) {
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<CertificateEntity> query = builder.createQuery(CertificateEntity.class);
        final Root<CertificateEntity> root = query.from(CertificateEntity.class);

//        query.where(builder.equal(root.get(CertificateEntity_.id.organization).get(OrganizationEntity_.ID), organizationID));
        query.where(builder.and(
                builder.equal(root.get(CertificateEntity_.id), certificateID),
                builder.equal(root.get(CertificateEntity_.MANAGING_ORGANIZATION).get(OrganizationEntity_.ID), organizationID)));

        return list(query);
    }
}
