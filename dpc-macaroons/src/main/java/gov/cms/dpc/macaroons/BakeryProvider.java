package gov.cms.dpc.macaroons;

import gov.cms.dpc.macaroons.annotations.PublicURL;
import gov.cms.dpc.macaroons.caveats.ExpirationCaveatSupplier;
import gov.cms.dpc.macaroons.caveats.ExpirationCaveatVerifier;
import gov.cms.dpc.macaroons.caveats.VersionCaveatSupplier;
import gov.cms.dpc.macaroons.caveats.VersionCaveatVerifier;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
import gov.cms.dpc.macaroons.thirdparty.IThirdPartyKeyStore;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Injection helper for building the {@link MacaroonBakery} which can be service injected
 */
@Singleton
public class BakeryProvider implements Provider<MacaroonBakery> {

    private final TokenPolicy tokenPolicy;
    private final IRootKeyStore store;
    private final IThirdPartyKeyStore thirdPartyKeyStore;
    private final String publicURL;
    private final BakeryKeyPair keyPair;

    @Inject
    public BakeryProvider(TokenPolicy tokenPolicy, IRootKeyStore store, IThirdPartyKeyStore thirdPartyKeyStore, @PublicURL String publicURI, BakeryKeyPair keyPair) {
        this.tokenPolicy = tokenPolicy;
        this.store = store;
        this.thirdPartyKeyStore = thirdPartyKeyStore;
        this.publicURL = publicURI;
        this.keyPair = keyPair;
    }

    @Override
    public MacaroonBakery get() {
        return new MacaroonBakery.MacaroonBakeryBuilder(publicURL, store, thirdPartyKeyStore)
                .withKeyPair(keyPair)
                .addDefaultCaveatSupplier(new VersionCaveatSupplier(tokenPolicy))
                .addDefaultCaveatSupplier(new ExpirationCaveatSupplier(tokenPolicy))
                .addDefaultVerifier(new VersionCaveatVerifier(tokenPolicy))
                .addDefaultVerifier(new ExpirationCaveatVerifier(tokenPolicy))
                .build();
    }
}
