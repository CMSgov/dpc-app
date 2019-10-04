package gov.cms.dpc.api.auth.macaroons;

import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.config.TokenPolicy;
import gov.cms.dpc.common.annotations.APIV1;
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

    private final DPCAPIConfiguration configuration;
    private final IRootKeyStore store;
    private final IThirdPartyKeyStore thirdPartyKeyStore;
    private final String publicURL;

    @Inject
    public BakeryProvider(DPCAPIConfiguration configuration, IRootKeyStore store, IThirdPartyKeyStore thirdPartyKeyStore, @APIV1 String publicURI) {
        this.configuration = configuration;
        this.store = store;
        this.thirdPartyKeyStore = thirdPartyKeyStore;
        this.publicURL = publicURI;
    }

    @Override
    public MacaroonBakery get() {
        final TokenPolicy tokenPolicy = this.configuration.getTokenPolicy();
        return new MacaroonBakery.MacaroonBakeryBuilder(publicURL, store, thirdPartyKeyStore)
                .addDefaultCaveatSupplier(new VersionCaveatSupplier(tokenPolicy))
                .addDefaultCaveatSupplier(new ExpirationCaveatSupplier(tokenPolicy))
                .addDefaultVerifier(new VersionCaveatVerifier(tokenPolicy))
                .addDefaultVerifier(new ExpirationCaveatVerifier(tokenPolicy))
                .build();
    }
}
