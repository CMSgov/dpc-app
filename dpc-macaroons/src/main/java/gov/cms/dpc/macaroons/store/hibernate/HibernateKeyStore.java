package gov.cms.dpc.macaroons.store.hibernate;

import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.store.IDKeyPair;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.store.hibernate.entities.RootKeyEntity;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * DB backed {@link IRootKeyStore} using Hibernate as the underlying ORM
 * This KeyStore generates a new RootKey for each ID that's passed in.
 * By default, it generates RootKeys that are valid for 1 year, but that will eventually be changed
 * This store assumes that the required tables and triggers are already setup.
 * Currently, we do the migration in the dpc-attribution module, but that will probably need to be improved.
 */
public class HibernateKeyStore implements IRootKeyStore {

    // Valid chars, which can be encoded as UTF-8
    private static final String VALID_PW_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+{}[]|:;<>?,./";
    private static final int ROOT_KEY_LENGTH = 24;

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
        final String rootKey = this.generateRootKey();

        final RootKeyEntity entity = new RootKeyEntity();
        final String idString = id.toString();
        entity.setId(idString);
        entity.setRootKey(rootKey);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setCreated(now);
        // TODO: Make this configurable. DPC-284
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

        return new IDKeyPair(idString, rootKey);
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

    /**
     * Securely generate a random password string
     * Borrowing from: https://stackoverflow.com/questions/29756660/create-random-password-using-java-securerandom-class
     *
     * @return - {@link String} root key
     */
    private String generateRootKey() {
        return RandomStringUtils.random(ROOT_KEY_LENGTH, 0, VALID_PW_CHARS.length(), false, false, VALID_PW_CHARS.toCharArray(), this.random);
    }
}
