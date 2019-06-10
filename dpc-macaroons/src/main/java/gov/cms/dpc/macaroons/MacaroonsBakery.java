package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import com.github.nitram509.jmacaroons.MacaroonsVerifier;
import gov.cms.dpc.macaroons.store.IRootKeyStore;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class MacaroonsBakery {

    public static final Charset CAVEAT_CHARSET = Charset.forName("UTF-8");
    private static final Base64.Encoder encoder = Base64.getUrlEncoder();
    private static final Base64.Decoder decoder = Base64.getUrlDecoder();

    private final String location;
    private final IRootKeyStore store;

    @Inject
    public MacaroonsBakery(@ServerLocation String location, IRootKeyStore store) {
        this.location = location;
        this.store = store;
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

    public void verifyMacaroon(Macaroon macaroon) {
        final MacaroonsVerifier verifier = new MacaroonsVerifier(macaroon);
        verifier.satisfyExact("organization_id = 0c527d2e-2e8a-4808-b11d-0fa06baf8254");

        // Get the macaroon secret from the store
        final String secret = this.store.get(macaroon.identifier);
        verifier.assertIsValid(secret);
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
}
