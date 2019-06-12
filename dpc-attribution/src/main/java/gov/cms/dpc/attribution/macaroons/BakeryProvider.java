package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.store.IRootKeyStore;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class BakeryProvider implements Provider<MacaroonBakery> {

    private final DPCAttributionConfiguration configuration;
    private final IRootKeyStore store;

    @Inject
    public BakeryProvider(DPCAttributionConfiguration configuration, IRootKeyStore store) {
        this.configuration = configuration;
        this.store = store;
    }

    @Override
    public MacaroonBakery get() {
        return new MacaroonBakery.MacaroonBakeryBuilder(configuration.getPublicServerURL(), store)
                .addDefaultCaveatSupplier(new VersionCaveatSupplier())
                .addDefaultCaveatSupplier(new ExpirationCaveatSupplier())
                .addDefaultVerifier(new VersionCaveatVerifier())
                .addDefaultVerifier(new ExpirationCaveatVerifier())
                .build();
    }
}
