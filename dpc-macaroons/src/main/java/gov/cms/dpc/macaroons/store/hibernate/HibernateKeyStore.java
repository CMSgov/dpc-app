package gov.cms.dpc.macaroons.store.hibernate;

import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.store.IDKeyPair;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.store.hibernate.entities.RootKeyEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.inject.Inject;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

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
        final byte[] key = new byte[24];
        this.random.nextBytes(key);

        final RootKeyEntity entity = new RootKeyEntity();
        final String idString = id.toString();
        entity.setId(idString);
        final String keyString = new String(key);
        entity.setRootKey(keyString);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setCreated(now);
        entity.setExpires(now.plus(1, ChronoUnit.YEARS));

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

        return new IDKeyPair(idString, keyString);
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
