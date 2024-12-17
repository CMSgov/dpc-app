package gov.cms.dpc.macaroons.store.hibernate;

import com.google.inject.Inject;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.helpers.SecretHelpers;
import gov.cms.dpc.macaroons.store.IDKeyPair;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.store.hibernate.entities.RootKeyEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * DB backed {@link IRootKeyStore} using Hibernate as the underlying ORM
 * This KeyStore generates a new RootKey for each ID that's passed in.
 * By default, it generates RootKeys that are valid for 1 year, but that will eventually be changed
 * This store assumes that the required tables and triggers are already setup.
 * Currently, we do the migration in the dpc-attribution module, but that will probably need to be improved.
 */
public class HibernateKeyStore implements IRootKeyStore {

    private final SessionFactory factory;
    private final SecureRandom random;

    @Inject
    public HibernateKeyStore(SessionFactory factory, SecureRandom random) {
        this.factory = factory;
        this.random = random;
    }

    @Override
    public IDKeyPair create() {
        final UUID id = UUID.randomUUID();
        final String rootKey = SecretHelpers.generateSecretKey(this.random);

        final RootKeyEntity entity = new RootKeyEntity();
        final String idString = id.toString();
        entity.setId(idString);
        entity.setRootKey(rootKey);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setCreated(now);
        // TODO: Make this configurable. DPC-284
        entity.setExpires(now.plusYears(1));

        try (final Session session = this.factory.openSession()) {
            final Transaction tx = session.beginTransaction();
            try {
                session.persist(entity);
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                throw new BakeryException(String.format("Cannot persist key to database. %s", e.getMessage()));
            }
        }

        return new IDKeyPair(idString, rootKey);
    }

    @Override
    public String generateKey() {
        return SecretHelpers.generateSecretKey(this.random);
    }

    @Override
    public String get(String macaroonID) {
        try (final Session session = this.factory.openSession()) {
            final RootKeyEntity entity = session.get(RootKeyEntity.class, macaroonID);
            if (entity == null) {
                throw new BakeryException(String.format("Cannot find root key for Macaroon ID %s", macaroonID));
            }

            return entity.getRootKey();
        }
    }

}
