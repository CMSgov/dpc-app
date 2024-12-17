package gov.cms.dpc.macaroons;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import gov.cms.dpc.macaroons.annotations.PublicURL;
import gov.cms.dpc.macaroons.caveats.ExpirationCaveatVerifier;
import gov.cms.dpc.macaroons.caveats.VersionCaveatVerifier;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
import gov.cms.dpc.macaroons.thirdparty.IThirdPartyKeyStore;



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
                .addDefaultVerifier(new VersionCaveatVerifier(tokenPolicy))
                .addDefaultVerifier(new ExpirationCaveatVerifier(tokenPolicy))
                .build();
    }
}
