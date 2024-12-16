package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.IpAddressEntity;
import gov.cms.dpc.api.entities.IpAddressEntity_;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

public class IpAddressDAO extends AbstractDAO<IpAddressEntity> {
    @Inject
    IpAddressDAO(DPCAuthManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

   public List<IpAddressEntity> fetchIpAddresses(UUID organizationID) {
       HibernateCriteriaBuilder builder = currentSession().getCriteriaBuilder();

       JpaCriteriaQuery<IpAddressEntity> query = builder.createQuery(IpAddressEntity.class);
       JpaRoot<IpAddressEntity> root = query.from(IpAddressEntity.class);
       query.where(builder.equal(root.get(IpAddressEntity_.organizationId.toString()), organizationID));

       return list(query);
   }

   public IpAddressEntity persistIpAddress(IpAddressEntity ipAddressEntity) {
        return persist(ipAddressEntity);
    }

    public void deleteIpAddress(IpAddressEntity ipAddress) {
        currentSession().delete(ipAddress);
    }
}
