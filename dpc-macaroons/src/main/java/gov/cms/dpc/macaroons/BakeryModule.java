package gov.cms.dpc.macaroons;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.store.hibernate.HibernateKeyStore;

import java.security.SecureRandom;

public class BakeryModule extends PrivateModule {

    public BakeryModule() {
        // Not used
    }

    @Override
    protected void configure() {
        bind(IRootKeyStore.class).to(HibernateKeyStore.class);
        expose(IRootKeyStore.class);
        expose(SecureRandom.class);
        bind(MacaroonBakery.class).toProvider(BakeryProvider.class);
        expose(MacaroonBakery.class);
    }

    @Provides
    SecureRandom provideRandom() {
        return new SecureRandom();
    }
}
