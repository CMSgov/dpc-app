package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.*;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.store.IDKeyPair;
import gov.cms.dpc.macaroons.store.IRootKeyStore;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class MacaroonBakery {

    public static final Charset CAVEAT_CHARSET = Charset.forName("UTF-8");
    private static final Base64.Encoder encoder = Base64.getUrlEncoder();
    private static final Base64.Decoder decoder = Base64.getUrlDecoder();

    private final String location;
    private final IRootKeyStore store;
    private final List<CaveatWrapper> defaultVerifiers;

    MacaroonBakery(String location, IRootKeyStore store, List<CaveatVerifier> defaultVerifiers) {
        this.location = location;
        this.store = store;
        this.defaultVerifiers = defaultVerifiers
                .stream()
                .map(CaveatWrapper::new)
                .collect(Collectors.toList());
    }

    /**
     * Create a {@link Macaroon} from a given list of {@link MacaroonCaveat}
     * Bakery only supports first-party caveats. Passing a third-party caveat (a {@link MacaroonCaveat} with a non-empty ({@link MacaroonCaveat#getLocation()}
     * will result in an {@link UnsupportedOperationException} being thrown
     *
     * @param caveats - {@link List} of {@link MacaroonCaveat} to add to the {@link Macaroon}
     * @return - {@link Macaroon} with given {@link MacaroonCaveat}
     * @throws UnsupportedOperationException - if a third-party caveat is passed
     */
    public Macaroon createMacaroon(List<MacaroonCaveat> caveats) {
        final IDKeyPair idKeyPair = store.create();
        final MacaroonsBuilder builder = new MacaroonsBuilder(location, idKeyPair.getKey(), idKeyPair.getId());

        addCaveats(builder, caveats);

        return builder.getMacaroon();
    }

    /**
     * Get the {@link Macaroon} caveats as a formatted list
     * See the {@link MacaroonCaveat} documentation for details on the underlying parsing format
     * If unable to parse the caveat, a {@link BakeryException} is thrown
     *
     * @param macaroon - {@link Macaroon} to retrieve caveats from
     * @return - {@link List} of {@link MacaroonCaveat} which are parsed from the underlying string representation
     * @throws BakeryException if unable to parse the caveats correctly
     */
    public List<MacaroonCaveat> getCaveats(Macaroon macaroon) {
        return Arrays.stream(macaroon.caveatPackets)
                .map(MacaroonCaveat::parseFromPacket)
                .collect(Collectors.toList());
    }

    /**
     * Add the given {@link MacaroonCaveat} to an existing {@link Macaroon}
     * This returns a new {@link Macaroon} with the provided caveats appended to the existing ones
     *
     * @param macaroon - {@link Macaroon} to retrieve base caveats from
     * @param caveats  - {@link MacaroonCaveat} list of caveats to append to existing ones
     * @return - {@link Macaroon} new macaroon with existing caveats as well as newly added ones
     */
    public Macaroon addCaveats(Macaroon macaroon, MacaroonCaveat... caveats) {
        final MacaroonsBuilder builder = MacaroonsBuilder.modify(macaroon);
        addCaveats(builder, Arrays.asList(caveats));

        return builder.getMacaroon();
    }

    /**
     * Verify the {@link Macaroon} using only the default verifiers
     *
     * @param macaroon - {@link Macaroon} to verify
     * @throws BakeryException if verification fails
     */
    public void verifyMacaroon(Macaroon macaroon) {
        verifyMacaroonImpl(macaroon, Collections.emptyList());
    }

    /**
     * Verify the {@link Macaroon} using both the default verifiers, as well as the ones provided in this method.
     * The provided {@link String} variables will be directly matched against the {@link MacaroonCaveat} string representation
     *
     * @param macaroon       - {@link Macaroon} to verify
     * @param exactVerifiers - {@link String} values to be directly matched against {@link MacaroonCaveat} values
     */
    public void verifyMacaroon(Macaroon macaroon, String... exactVerifiers) {
        // Convert the String checks into a caveat wrapper by generating a lambda which handles teh actual checking
        final List<CaveatWrapper> verifiers = Arrays.stream(exactVerifiers)
                .map(ev -> new CaveatWrapper((caveat) -> {
                    if (caveat.toString().equals(ev)) {
                        return Optional.empty();
                    }
                    return Optional.of("Caveat is not satisfied");
                }))
                .collect(Collectors.toList());

        verifyMacaroonImpl(macaroon, verifiers);
    }

    /**
     * Verify the {@link Macaroon} using both the default verifiers, as well as the ones provided in this method.
     *
     * @param macaroon        - {@link Macaroon} to verify
     * @param caveatVerifiers - {@link CaveatVerifier} which will be executed against the {@link MacaroonCaveat}
     */
    public void verifyMacaroon(Macaroon macaroon, CaveatVerifier... caveatVerifiers) {
        final List<CaveatWrapper> verifiers = Arrays.stream(caveatVerifiers)
                .map(CaveatWrapper::new)
                .collect(Collectors.toList());
        verifyMacaroonImpl(macaroon, verifiers);
    }

    /**
     * Convert the {@link Macaroon} to a {@link MacaroonCaveat##CAVEAT_CHARSET} byte format.
     * Optionally, the Macaroon can be base64 (URL-safe) encoded before returning.
     *
     * @param macaroon     - {@link Macaroon} to serialize
     * @param base64Encode - {@code true} Macaroon bytes are base64 (URL-safe) encoded. {@link false} Macaroon bytes are returned directly
     * @return - Macaroon byte array
     */
    public byte[] serializeMacaroon(Macaroon macaroon, boolean base64Encode) {
        final byte[] macaroonBytes = macaroon.serialize(MacaroonVersion.SerializationVersion.V2_JSON).getBytes(CAVEAT_CHARSET);
        if (base64Encode) {
            return encoder.encode(macaroonBytes);
        }
        return macaroonBytes;
    }

    /**
     * Deserialize {@link Macaroon} from provided {@link String} value.
     * This {@link String} can be either base64 (URL-safe) encoded or a direct representation (e.g. a JSON string)
     *
     * @param serializedString - {@link String} to deserialize from
     * @return - {@link Macaroon} deserialized from {@link String}
     */
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
                    // TODO: Eventually we'll need to support third-party caveats
                    if (caveat.isThirdParty()) {
                        throw new UnsupportedOperationException("We do not currently support third-party caveats");
                    }
                    builder.add_first_party_caveat(caveat.toString());
                });
    }

    private void verifyMacaroonImpl(Macaroon macaroon, List<CaveatWrapper> verifiers) {
        final MacaroonsVerifier verifier = new MacaroonsVerifier(macaroon);
        // Add the default caveats and the provided ones
        this.defaultVerifiers.forEach(v -> verifier.satisfyGeneral(v::verifyCaveat));
        verifiers
                .forEach(v -> verifier.satisfyGeneral(v::verifyCaveat));

        // Get the macaroon secret from the store
        final String secret = this.store.get(macaroon.identifier);
        try {
            verifier.assertIsValid(secret);
        } catch (MacaroonValidationException e) {
            throw new BakeryException(e.getMessage());
        }
    }

    /**
     * Builder for {@link MacaroonsBuilder}
     */
    public static class MacaroonBakeryBuilder {

        private final List<CaveatVerifier> caveatVerifiers;
        private final String serverLocation;
        private final IRootKeyStore rootKeyStore;

        /**
         * Default parameters for {@link MacaroonBakery}
         *
         * @param serverLocation - {@link String} Server URL to use when creating {@link Macaroon}
         * @param keyStore       - {@link IRootKeyStore} to use for handling {@link Macaroon} secret keys
         */
        public MacaroonBakeryBuilder(String serverLocation, IRootKeyStore keyStore) {
            this.caveatVerifiers = new ArrayList<>();
            this.serverLocation = serverLocation;
            this.rootKeyStore = keyStore;
        }

        /**
         * Add {@link CaveatVerifier} which will be applied, by default, to every {@link Macaroon} being verified
         *
         * @param caveatVerifier - {@link CaveatVerifier} to apply to each {@link Macaroon}
         * @return - {@link MacaroonBakeryBuilder}
         */
        public MacaroonBakeryBuilder addDefaultVerifier(CaveatVerifier caveatVerifier) {
            this.caveatVerifiers.add(caveatVerifier);
            return this;
        }

        /**
         * Build the {@link MacaroonBakery}
         *
         * @return - {@link MacaroonBakery}
         */
        public MacaroonBakery build() {
            return new MacaroonBakery(this.serverLocation, this.rootKeyStore, this.caveatVerifiers);
        }
    }
}
