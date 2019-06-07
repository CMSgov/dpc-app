package gov.cms.dpc.macaroons;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;

import java.security.SecureRandom;

public class BakeryModule extends PrivateModule {

    public BakeryModule() {
        // Not used
    }

    @Override
    protected void configure() {
        bind(IRootKeyStore.class).to(MemoryRootKeyStore.class);
        expose(IRootKeyStore.class);
        bind(MacaroonsBakery.class);
        expose(MacaroonsBakery.class);
    }

    @Provides
    SecureRandom provideRandom() {
        return new SecureRandom();
    }
}
