package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.config.TokenPolicy;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.IThirdPartyKeyStore;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Injection helper for building the {@link MacaroonBakery} which can be service injected
 */
@Singleton
public class BakeryProvider implements Provider<MacaroonBakery> {

    private final DPCAttributionConfiguration configuration;
    private final IRootKeyStore store;
    private final IThirdPartyKeyStore thirdPartyKeyStore;

    @Inject
    public BakeryProvider(DPCAttributionConfiguration configuration, IRootKeyStore store, IThirdPartyKeyStore thirdPartyKeyStore) {
        this.configuration = configuration;
        this.store = store;
        this.thirdPartyKeyStore = thirdPartyKeyStore;
    }

    @Override
    public MacaroonBakery get() {
        final TokenPolicy tokenPolicy = this.configuration.getTokenPolicy();
        return new MacaroonBakery.MacaroonBakeryBuilder(configuration.getPublicServerURL(), store, thirdPartyKeyStore)
                .addDefaultCaveatSupplier(new VersionCaveatSupplier(tokenPolicy))
                .addDefaultCaveatSupplier(new ExpirationCaveatSupplier(tokenPolicy))
                .addDefaultVerifier(new VersionCaveatVerifier(tokenPolicy))
                .addDefaultVerifier(new ExpirationCaveatVerifier(tokenPolicy))
                .build();
    }
}
