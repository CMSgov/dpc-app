package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EndpointDAO extends AbstractDAO<EndpointEntity> {

    @Inject
    EndpointDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public Optional<EndpointEntity> fetchEndpoint(UUID endpointID) {
        return Optional.ofNullable(get(endpointID));
    }

    public List<EndpointEntity> findByOrganization(UUID organizationID) {
        // Build a selection query to get records from the database
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<EndpointEntity> query = builder.createQuery(EndpointEntity.class);
        final Root<EndpointEntity> root = query.from(EndpointEntity.class);
        query.select(root);


        query.where(builder.equal(root.get("organization").get("id"), organizationID));

        return list(query);
    }
}
