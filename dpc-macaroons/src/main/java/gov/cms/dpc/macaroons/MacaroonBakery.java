package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.*;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.store.IRootKeyStore;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class MacaroonBakery {

    public static final Charset CAVEAT_CHARSET = Charset.forName("UTF-8");
    private static final Base64.Encoder encoder = Base64.getUrlEncoder();
    private static final Base64.Decoder decoder = Base64.getUrlDecoder();

    private final String location;
    private final IRootKeyStore store;
    private final List<String> defaultVerifiers;

    MacaroonBakery(String location, IRootKeyStore store, List<String> defaultVerifiers) {
        this.location = location;
        this.store = store;
        this.defaultVerifiers = defaultVerifiers;
    }

    public Macaroon createMacaroon(List<MacaroonCaveat> caveats) {
        final MacaroonsBuilder builder = new MacaroonsBuilder(location, store.create(), "0");

        addCaveats(builder, caveats);

        return builder.getMacaroon();
    }

    public List<MacaroonCaveat> getCaveats(Macaroon macaroon) {
        return Arrays.stream(macaroon.caveatPackets)
                .map(MacaroonCaveat::parseFromPacket)
                .collect(Collectors.toList());
    }

    public Macaroon addCaveats(Macaroon macaroon, MacaroonCaveat... caveats) {
        final MacaroonsBuilder builder = MacaroonsBuilder.modify(macaroon);
        addCaveats(builder, Arrays.asList(caveats));

        return builder.getMacaroon();
    }

    public void verifyMacaroon(Macaroon macaroon, String... caveatVerifiers) {
        final MacaroonsVerifier verifier = new MacaroonsVerifier(macaroon);
        // Add the default caveats and the provided ones
        this.defaultVerifiers.forEach(verifier::satisfyExact);
        Arrays.stream(caveatVerifiers).forEach(verifier::satisfyExact);

        // Get the macaroon secret from the store
        final String secret = this.store.get(macaroon.identifier);
        try {
            verifier.assertIsValid(secret);
        } catch (MacaroonValidationException e) {
            throw new BakeryException(e.getMessage());
        }
    }

    public byte[] serializeMacaroon(Macaroon macaroon, boolean base64Encode) {
        final byte[] macaroonBytes = macaroon.serialize(MacaroonVersion.SerializationVersion.V2_JSON).getBytes(CAVEAT_CHARSET);
        if (base64Encode) {
            return encoder.encode(macaroonBytes);
        }
        return macaroonBytes;
    }

    public Macaroon deserializeMacaroon(String serializedString) {
        // Determine if we're Base64 encoded or not
        byte[] decodedString;
        // For a V2 JSON macaroon, either '{' or '[' will be the starting value, so we check for the base64 encoded value
        final char indexChar = serializedString.charAt(0);
        if (indexChar == 'e' || indexChar == 'W') {
            decodedString = decoder.decode(serializedString.getBytes(CAVEAT_CHARSET));
        } else {
            decodedString = serializedString.getBytes(CAVEAT_CHARSET);
        }
        return MacaroonsBuilder.deserialize(new String(decodedString));
    }

    private void addCaveats(MacaroonsBuilder builder, List<MacaroonCaveat> caveats) {
        caveats
                .forEach(caveat -> {
                    // We'll need to expand this to support third-party caveats, at some point
                    if (caveat.isThirdParty()) {
                        throw new UnsupportedOperationException("We do not currently support third-party caveats");
                    }
                    builder.add_first_party_caveat(caveat.getCaveatText());
                });
    }

    public static class MacaroonBakeryBuilder {

        private final List<String> caveatVerifiers;
        private final String serverLocation;
        private final IRootKeyStore rootKeyStore;

        public MacaroonBakeryBuilder(String serverLocation, IRootKeyStore keyStore) {
            this.caveatVerifiers = new ArrayList<>();
            this.serverLocation = serverLocation;
            this.rootKeyStore = keyStore;
        }

        public MacaroonBakeryBuilder addDefaultVerifier(String caveatVerifier) {
            this.caveatVerifiers.add(caveatVerifier);
            return this;
        }

        public MacaroonBakery build() {
            return new MacaroonBakery(this.serverLocation, this.rootKeyStore, this.caveatVerifiers);
        }
    }
}
