package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.IpAddressEntity;
import gov.cms.dpc.api.entities.IpAddressEntity_;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

public class IpAddressDAO extends AbstractDAO<IpAddressEntity> {
    @Inject
    IpAddressDAO(DPCAuthManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

   public List<IpAddressEntity> fetchIpAddresses(UUID organizationID) {
       CriteriaBuilder builder = currentSession().getCriteriaBuilder();

       CriteriaQuery<IpAddressEntity> query = builder.createQuery(IpAddressEntity.class);
       Root<IpAddressEntity> root = query.from(IpAddressEntity.class);
       query.where(builder.equal(root.get(IpAddressEntity_.organizationId), organizationID));

       return list(query);
   }

   public IpAddressEntity persistIpAddress(IpAddressEntity ipAddressEntity) {
        return persist(ipAddressEntity);
    }

    public void deleteIpAddress(IpAddressEntity ipAddress) {
        currentSession().delete(ipAddress);
    }
}
