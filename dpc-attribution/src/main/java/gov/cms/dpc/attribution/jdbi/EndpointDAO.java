package gov.cms.dpc.attribution.jdbi;

import com.google.common.collect.Lists;
import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;
import jakarta.inject.Inject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EndpointDAO extends AbstractDAO<EndpointEntity> {

    @Inject
    EndpointDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public EndpointEntity persistEndpoint(EndpointEntity endpointEntity) {
        return persist(endpointEntity);
    }

    public Optional<EndpointEntity> fetchEndpoint(UUID endpointID) {
        return Optional.ofNullable(get(endpointID));
    }

    public List<EndpointEntity> endpointSearch(UUID organizationID, UUID endpointId) {
        // Build a selection query to get records from the database
        final CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        final CriteriaQuery<EndpointEntity> query = builder.createQuery(EndpointEntity.class);
        final Root<EndpointEntity> root = query.from(EndpointEntity.class);
        query.select(root);

        final List<Predicate> predicates = Lists.newArrayList();

        if(organizationID!=null){
            predicates.add(builder.equal(root.get("organization").get("id"), organizationID));
        }
        if(endpointId!=null){
            predicates.add(builder.equal(root.get("id"), endpointId));
        }
        query.where(builder.and(predicates.toArray(Predicate[]::new)));

        return list(query);
    }

    public void deleteEndpoint(EndpointEntity endpointEntity) {
        currentSession().delete(endpointEntity);
    }
}
