package gov.cms.dpc.macaroons;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.store.hibernate.HibernateKeyStore;

import java.security.SecureRandom;

public class BakeryModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(IRootKeyStore.class).to(HibernateKeyStore.class);
        expose(IRootKeyStore.class);
        expose(SecureRandom.class);
        bind(MacaroonBakery.class).toProvider(BakeryProvider.class).in(Scopes.SINGLETON);
        expose(MacaroonBakery.class);
    }

    @Provides
    SecureRandom provideRandom() {
        return new SecureRandom();
    }
}
