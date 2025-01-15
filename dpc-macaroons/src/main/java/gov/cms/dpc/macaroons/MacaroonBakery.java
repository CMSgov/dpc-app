package gov.cms.dpc.macaroons;

import com.codahale.xsalsa20poly1305.SecretBox;
import com.github.nitram509.jmacaroons.*;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.helpers.ByteBufferBackedInputStream;
import gov.cms.dpc.macaroons.helpers.VarInt;
import gov.cms.dpc.macaroons.store.IDKeyPair;
import gov.cms.dpc.macaroons.store.IRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
import gov.cms.dpc.macaroons.thirdparty.IThirdPartyKeyStore;
import org.apache.commons.lang3.tuple.Pair;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * {@link Macaroon} bakery (factory) which abstracts most of the interactions with Macaroons.
 * <p>
 * This provides a simpler interface for handling creation/verification/delegation
 * It currently does NOT support third-party caveats. Which will come in DPC-286
 */
public class MacaroonBakery {

    private static final Charset CAVEAT_CHARSET = StandardCharsets.UTF_8;
    private static final Base64.Encoder encoder = Base64.getUrlEncoder();
    private static final Base64.Decoder decoder = Base64.getUrlDecoder();

    private final String location;
    private final IRootKeyStore store;
    private final BakeryKeyPair keyPair;
    private final List<CaveatWrapper> defaultVerifiers;
    private final List<CaveatSupplier> defaultSuppliers;
    private final IThirdPartyKeyStore thirdPartyKeyStore;

    MacaroonBakery(String location,
                   IRootKeyStore store,
                   IThirdPartyKeyStore thirdPartyKeyStore,
                   BakeryKeyPair keyPair,
                   List<CaveatVerifier> defaultVerifiers,
                   List<CaveatSupplier> defaultSuppliers) {
        this.location = location;
        this.store = store;
        this.defaultVerifiers = defaultVerifiers
                .stream()
                .map(CaveatWrapper::new)
                .collect(Collectors.toList());
        this.defaultSuppliers = defaultSuppliers;
        this.thirdPartyKeyStore = thirdPartyKeyStore;
        this.keyPair = keyPair;

        // Add the current location and the custom `local` location to the TP key store
        final byte[] keyBytes = this.keyPair.getPublicKey();
        this.thirdPartyKeyStore.setPublicKey(location, keyBytes);
        this.thirdPartyKeyStore.setPublicKey("local", keyBytes);
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

        List<MacaroonCaveat> defaultCaveats = this.defaultSuppliers
                .stream()
                .map(CaveatSupplier::get)
                .collect(Collectors.toList());
        defaultCaveats.addAll(caveats);

        addCaveats(builder, defaultCaveats);

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
    public static List<MacaroonCaveat> getCaveats(Macaroon macaroon) {
        List<MacaroonCaveat> caveats = new ArrayList<>();

        MacaroonCaveat currentCaveat = new MacaroonCaveat();

        for (final CaveatPacket packet : macaroon.caveatPackets) {
            // If we've encountered a CID, add the current caveat to our list and start a new one
            if (packet.type == CaveatPacket.Type.cid) {
                if (currentCaveat.getRawCaveat() != null) {
                    caveats.add(currentCaveat);
                    currentCaveat = new MacaroonCaveat();
                }

                currentCaveat.setRawCaveat(packet.getRawValue());
            } else if (packet.type == CaveatPacket.Type.cl) {
                currentCaveat.setLocation(packet.getValueAsText());
            } else {
                currentCaveat.setVerificationID(packet.getRawValue());
            }
        }

        if (currentCaveat.getRawCaveat() != null) {
            caveats.add(currentCaveat);
        }

        return caveats;
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
     * Verify a {@link Macaroon} using only the default verifiers
     *
     * @param macaroons - {@link List} of {@link Macaroon} to verify along with any discharges
     * @throws BakeryException if verification fails
     */
    public void verifyMacaroon(List<Macaroon> macaroons) {
        verifyMacaroonImpl(macaroons, Collections.emptyList());
    }

    /**
     * Verify the {@link Macaroon}s using both the default verifiers, as well as the ones provided in this method.
     * The provided {@link String} variables will be directly matched against the {@link MacaroonCaveat} string representation
     *
     * @param macaroons      - {@link List} of {@link Macaroon} to verify, along with any discharges
     * @param exactVerifiers - {@link String} values to be directly matched against {@link MacaroonCaveat} values
     */
    public void verifyMacaroon(List<Macaroon> macaroons, String... exactVerifiers) {
        // Convert the String checks into a caveat wrapper by generating a lambda which handles teh actual checking
        final List<CaveatWrapper> verifiers = Arrays.stream(exactVerifiers)
                .map(ev -> new CaveatWrapper(caveat -> {
                    if (caveat.toString().equals(ev)) {
                        return Optional.empty();
                    }
                    return Optional.of("Caveat is not satisfied");
                }))
                .collect(Collectors.toList());

        verifyMacaroonImpl(macaroons, verifiers);
    }

    /**
     * Verify the {@link Macaroon} using both the default verifiers, as well as the ones provided in this method.
     *
     * @param macaroons       - {@link List} of {@link Macaroon} to verify, along with any discharges
     * @param caveatVerifiers - {@link CaveatVerifier} which will be executed against the {@link MacaroonCaveat}
     */
    public void verifyMacaroon(List<Macaroon> macaroons, CaveatVerifier... caveatVerifiers) {
        final List<CaveatWrapper> verifiers = Arrays.stream(caveatVerifiers)
                .map(CaveatWrapper::new)
                .collect(Collectors.toList());
        verifyMacaroonImpl(macaroons, verifiers);
    }

    /**
     * Convert the {@link Macaroon} to the underlying byte format.
     * Optionally, the Macaroon can be base64 (URL-safe) encoded before returning.
     *
     * @param macaroon     - {@link Macaroon} to serialize
     * @param base64Encode - {@code true} Macaroon bytes are base64 (URL-safe) encoded. {@code false} Macaroon bytes are returned directly
     * @return - Macaroon byte array
     */
    public byte[] serializeMacaroon(Macaroon macaroon, boolean base64Encode) {
        return serializeMacaroon(Collections.singletonList(macaroon), base64Encode);
    }

    /**
     * Convert the {@link List} of {@link Macaroon} to the underlying byte format.
     * Optionally, the Macaroon can be base64 (URL-safe) encoded before returning.
     *
     * @param macaroons    - {@link List} of {@link Macaroon} to serialize
     * @param base64Encode - {@code true} Macaroon bytes are base64 (URL-safe) encoded. {@code false} Macaroon bytes are returned directly
     * @return - Macaroon byte array
     */
    public byte[] serializeMacaroon(List<Macaroon> macaroons, boolean base64Encode) {

        final byte[] serializedBytes = macaroons.stream().map(m -> m.serialize(MacaroonVersion.SerializationVersion.V2_JSON)).collect(Collectors.joining(",", "[", "]")).getBytes(CAVEAT_CHARSET);
        if (base64Encode) {
            return encoder.encode(serializedBytes);
        }
        return serializedBytes;
    }

    /**
     * Deserialize a {@link List} of {@link Macaroon} from provided {@link String} value.
     * This {@link String} can be either base64 (URL-safe) encoded or a direct representation (e.g. a JSON string)
     *
     * @param serializedString - {@link String} to deserialize from
     * @return - {@link List} of {@link Macaroon} deserialized from {@link String}
     */
    public static List<Macaroon> deserializeMacaroon(String serializedString) {
        if (serializedString.isEmpty()) {
            throw new BakeryException("Cannot deserialize empty string");
        }
        // Determine if we're Base64 encoded or not
        byte[] decodedString;
        // For a JSON macaroon, either '{' or '[' will be the starting value, for V1 binary it's 'T', so we check for the base64 encoded value
        final char indexChar = serializedString.charAt(0);
        try{
            if (indexChar == 'e' || indexChar == 'W' || indexChar == 'T') {
                decodedString = decoder.decode(serializedString.getBytes(CAVEAT_CHARSET));
            } else {
                decodedString = serializedString.getBytes(CAVEAT_CHARSET);
            }
        }
        catch (Exception e) {
            throw new BakeryException("Cannot deserialize Macaroon", e);
        }
        try {
            return MacaroonsBuilder.deserialize(new String(decodedString, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BakeryException("Cannot deserialize Macaroon", e);
        }
    }

    /**
     * Discharge are provided {@link Macaroon} using the provided {@link MacaroonDischarger}
     * This will recursively decrypt the provided caveats, verify them and discharge any returned caveats.
     * The returned Macaroons are bound to the 'root macaroon' which is the first member of the input {@link List}
     *
     * @param macaroons  - {@link List} of {@link Macaroon} to discharge
     * @param discharger - {@link MacaroonDischarger} which handles processing the caveats
     * @return - {@link List} of discharged {@link Macaroon}s
     */
    public List<Macaroon> dischargeAll(List<Macaroon> macaroons, MacaroonDischarger discharger) {
        final List<Macaroon> macaroons1 = dischargeAllImpl(macaroons, discharger);

        final Macaroon rootMacaroon = macaroons1.get(0);

        List<Macaroon> boundMacaroons = new ArrayList<>();

        // Add the root Macaroon, which everything else binds to
        boundMacaroons.add(rootMacaroon);

        final MacaroonsBuilder builder = new MacaroonsBuilder(rootMacaroon);

        // Bind each of the discharge macaroons to the root macaroon
        macaroons1.subList(1, macaroons1.size())
                .stream()
                .map(builder::prepare_for_request)
                .map(MacaroonsBuilder::getMacaroon)
                .forEach(boundMacaroons::add);

        return boundMacaroons;
    }

    public Macaroon discharge(MacaroonCaveat caveat, byte[] payload) {
        final Pair<String, MacaroonCondition> stringMacaroonCaveatPair = decodeCaveat(caveat.getRawCaveat());

        // Create a discharge macaroon
        return MacaroonsBuilder.create("", stringMacaroonCaveatPair.getLeft(), caveat.getRawCaveat());
    }

    /**
     * Implementation of discharge logic.
     * Recursively iterates through the provided {@link List} of {@link Macaroon} and discharges any third-party caveats
     * The returned Macaroon List is not bound to the root macaroon
     *
     * @param macaroons  - {@link List} of {@link Macaroon} to discharge
     * @param discharger - {@link MacaroonDischarger} which handles the actual caveat discharging.
     * @return - {@link List} of discharged (but un-bound) {@link Macaroon}
     */
    private List<Macaroon> dischargeAllImpl(List<Macaroon> macaroons, MacaroonDischarger discharger) {
        if (macaroons.isEmpty()) {
            throw new BakeryException("No macaroons to discharge");
        }

        List<Macaroon> discharged = new ArrayList<>(macaroons);

        Queue<MacaroonCaveat> needCaveat = new ArrayDeque<>();
        Map<String, Boolean> haveCaveat = new HashMap<>();
        macaroons.subList(1, macaroons.size())
                .forEach(macaroon -> haveCaveat.put(macaroon.identifier, true));


        // addCaveats adds any required third party caveats to the need slice
        // that aren't already present .
        Consumer<Macaroon> addCaveats = macaroon -> MacaroonBakery.getCaveats(macaroon)
                .stream()
                .filter(cav -> cav.getVerificationID().length > 1 && !haveCaveat.containsKey(cav.toString()))
                .forEach(needCaveat::add);

        macaroons.forEach(addCaveats);

        // Pop each caveat off the queue and process it
        while (!needCaveat.isEmpty()) {
            final MacaroonCaveat cav = needCaveat.poll();
            final Macaroon dm = discharger.getDischarge(cav, null);
            discharged.add(dm);
            addCaveats.accept(dm);
        }

        return discharged;
    }

    private void addCaveats(MacaroonsBuilder builder, List<MacaroonCaveat> caveats) {
        caveats
                .forEach(caveat -> {
                    if (caveat.isThirdParty()) {
                        // Generate a new random key
                        final String caveatKey = this.store.generateKey();
                        final byte[] encryptedCaveat = encodeThirdPartyCaveat(caveat, caveatKey);
                        builder.add_third_party_caveat(caveat.getLocation(), caveatKey, encryptedCaveat);

                    } else {
                        builder.add_first_party_caveat(caveat.toString());
                    }
                });
    }

    /**
     * Encodes a third-party caveat using the public key of the third-party
     * <p>
     * The caveat format is:
     * version (1 byte)
     * Prefix of third party public key (4-bytes)
     * First party public key (32-bytes)
     * Random nonce (24-bytes)
     * Secret part of message
     *
     * @param caveat - {@link MacaroonCaveat} to encode
     * @return - {@link Byte} encrypted third-party caveat
     * @see MacaroonBakery#encodeSecretPart(byte[], byte[], byte[], String, String)
     */
    private byte[] encodeThirdPartyCaveat(MacaroonCaveat caveat, String rootKey) {

        final SecureRandom random = new SecureRandom();

        final byte[] nonce = new byte[MacaroonsConstants.MACAROON_SECRET_NONCE_BYTES];
        random.nextBytes(nonce);

        final byte[] thirdPartyKeyBytes = this.thirdPartyKeyStore.getPublicKey(caveat.getLocation())
                .orElseThrow(() -> new BakeryException(String.format("Cannot find public key for %s", caveat.getLocation())));

        final byte[] privateKeyBytes = this.keyPair.getPrivateKey();
        final byte[] publicKeyBytes = this.keyPair.getPublicKey();

        final byte[] secretPart = encodeSecretPart(thirdPartyKeyBytes,
                privateKeyBytes,
                nonce,
                rootKey,
                caveat.toString());

//         Create the caveat header
        final ByteBuffer fullMessage = ByteBuffer.allocate(1
                + 4
                + MacaroonsConstants.MACAROON_SECRET_KEY_BYTES
                + MacaroonsConstants.MACAROON_SECRET_NONCE_BYTES
                + secretPart.length);

        fullMessage.put((byte) 2);
        fullMessage.put(Arrays.copyOfRange(thirdPartyKeyBytes, 0, 4));
        fullMessage.put(publicKeyBytes);
        fullMessage.put(nonce);
        fullMessage.put(secretPart);
        // Reset the buffer pointer
        fullMessage.flip();

        return fullMessage.array();
    }

    /**
     * Encodes the caveat value using a {@link SecretBox} using the provided third-party public key and first-party private key.
     * <p>
     * The box format is:
     * version (1 byte)
     * varint of encoded root key length
     * root key (24-bytes)
     * caveat string
     *
     * @param thirdPartyKey - {@link Byte} third-party public key to use
     * @param privateKey    - {@link Byte} first-party private key to use
     * @param nonce         - {@link Byte}
     * @param rootKey       - {@link String} root key use to validate caveat
     * @param caveat        - {@link String} caveat value
     * @return - {@link Byte} encrypted message via {@link SecretBox#seal(byte[], byte[])}
     */
    static byte[] encodeSecretPart(byte[] thirdPartyKey, byte[] privateKey, byte[] nonce, String rootKey, String caveat) {
        // Convert the rootKey to bytes (preserving encoding)
        final byte[] keyBytes = rootKey.getBytes(MacaroonsConstants.RAW_BYTE_CHARSET);
        final byte[] messageBytes = caveat.getBytes(CAVEAT_CHARSET);

        // Root key length as a varint
        final byte[] keyLengthBytes = VarInt.writeUnsignedVarInt(keyBytes.length);

        // Allocate a byte buffer that is the size of the version (1 byte), the varint length of rootKey, rootKey and message
        final ByteBuffer msgBuffer = ByteBuffer.allocate(1
                + keyLengthBytes.length
                + keyBytes.length
                + messageBytes.length);

        msgBuffer.put((byte) 2);
        msgBuffer.put(keyLengthBytes);
        msgBuffer.put(keyBytes);
        msgBuffer.put(messageBytes);
        // Reset the buffer pointer
        msgBuffer.flip();

        final SecretBox secretBox = new SecretBox(thirdPartyKey, privateKey);
        return secretBox.seal(nonce, msgBuffer.array());
    }

    /**
     * Decodes the encrypted caveat using the private key which corresponds to the public key signature provided in the caveat structure
     *
     * @param encryptedCaveat - {@link Byte} array of encrypted caveat
     * @return - {@link Pair} of {@link String} caveat key and {@link MacaroonCondition} to validate
     * @see MacaroonBakery#encodeThirdPartyCaveat(MacaroonCaveat, String)
     */
    private Pair<String, MacaroonCondition> decodeCaveat(byte[] encryptedCaveat) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedCaveat);
        // Advance by one to skip the version
        byteBuffer.get();

        // Get the first 4 bytes of the caveat public key
        byte[] caveatKeySignature = new byte[4];
        byteBuffer.get(caveatKeySignature);

        byte[] pubKeySig = Arrays.copyOfRange(this.keyPair.getPublicKey(), 0, 4);
        if (!safeEquals(caveatKeySignature, pubKeySig)) {
            throw new BakeryException("Public key mismatch");
        }

        // Get the first party public key
        byte[] firstPartyPublicKey = new byte[MacaroonsConstants.MACAROON_SECRET_KEY_BYTES];
        byte[] nonce = new byte[MacaroonsConstants.MACAROON_SECRET_NONCE_BYTES];
        byteBuffer.get(firstPartyPublicKey);
        byteBuffer.get(nonce);


        // Decrypt the secret part
        final byte[] privateKeyBytes = this.keyPair.getPrivateKey();

        final SecretBox box = new SecretBox(firstPartyPublicKey, privateKeyBytes);

        final byte[] msg = new byte[byteBuffer.remaining()];
        byteBuffer.get(msg);
        final byte[] secretPart = box.open(nonce, msg)
                .orElseThrow(() -> new BakeryException("Cannot decrypt secret part of caveat"));

        return decodeCaveatSecretPart(secretPart);
    }

    /**
     * Decodes the secret part of the provided caveat
     *
     * @param secretPart - {@link Byte} array of secret part of caveat
     * @return - {@link Pair} of {@link String} caveat key and {@link MacaroonCondition} to validate
     * @see MacaroonBakery#encodeSecretPart(byte[], byte[], byte[], String, String)
     */
    private static Pair<String, MacaroonCondition> decodeCaveatSecretPart(byte[] secretPart) {
        final ByteBuffer buffer = ByteBuffer.wrap(secretPart);
        // Advance because we already know the version
        buffer.get();
        final int rootKeyLength;
        try {
            try (ByteBufferBackedInputStream is = new ByteBufferBackedInputStream(buffer);
                 final DataInputStream ds = new DataInputStream(is)) {
                rootKeyLength = VarInt.readUnsignedVarInt(ds);
            }
        } catch (IOException e) {
            throw new BakeryException("Cannot read root key length", e);
        }

        final byte[] rootKey = new byte[rootKeyLength];
        buffer.get(rootKey);
        // Allocate space for the remaining bytes
        final byte[] msg = new byte[buffer.remaining()];
        buffer.get(msg);
        final MacaroonCondition caveat = MacaroonCondition.parseFromString(new String(msg, MacaroonsConstants.IDENTIFIER_CHARSET));

        return Pair.of(new String(rootKey, MacaroonsConstants.IDENTIFIER_CHARSET), caveat);
    }

    private void verifyMacaroonImpl(List<Macaroon> macaroons, List<CaveatWrapper> verifiers) {
        final Macaroon rootMacaroon = macaroons.get(0);
        final List<Macaroon> dischargeMacaroons = macaroons.subList(1, macaroons.size());
        final MacaroonsVerifier verifier = new MacaroonsVerifier(rootMacaroon);
        // Add the default caveats and the provided ones
        this.defaultVerifiers.forEach(v -> verifier.satisfyGeneral(v::verifyCaveat));
        verifiers
                .forEach(v -> verifier.satisfyGeneral(v::verifyCaveat));

        // Add any discharge Macaroon
        dischargeMacaroons.forEach(verifier::satisfy3rdParty);
        // Get the macaroon secret from the store
        final String secret = this.store.get(rootMacaroon.identifier);
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

        private final String serverLocation;
        private final IRootKeyStore rootKeyStore;
        private final IThirdPartyKeyStore thirdPartyKeyStore;
        private final List<CaveatVerifier> caveatVerifiers;
        private final List<CaveatSupplier> caveatSuppliers;
        private BakeryKeyPair keyPair;

        /**
         * Default parameters for {@link MacaroonBakery}
         *
         * @param serverLocation     - {@link String} Server URL to use when creating {@link Macaroon}
         * @param keyStore           - {@link IRootKeyStore} to use for handling {@link Macaroon} secret keys
         * @param thirdPartyKeyStore - {@link IThirdPartyKeyStore} to use for handling public keys of third parties
         */
        public MacaroonBakeryBuilder(String serverLocation, IRootKeyStore keyStore, IThirdPartyKeyStore thirdPartyKeyStore) {
            this.serverLocation = serverLocation;
            this.rootKeyStore = keyStore;
            this.thirdPartyKeyStore = thirdPartyKeyStore;
            this.caveatVerifiers = new ArrayList<>();
            this.caveatSuppliers = new ArrayList<>();
            this.keyPair = null;
        }

        /**
         * Add a {@link CaveatSupplier} which will be applied to every {@link MacaroonCaveat} being created
         * Note: These caveats will NOT be added to any {@link Macaroon} which is cloned from an existing {@link Macaroon}
         *
         * @param caveatSupplier - {@link CaveatSupplier} which generates caveat
         * @return - {@link MacaroonBakeryBuilder}
         */
        MacaroonBakeryBuilder addDefaultCaveatSupplier(CaveatSupplier caveatSupplier) {
            this.caveatSuppliers.add(caveatSupplier);
            return this;
        }

        /**
         * Add {@link CaveatVerifier} which will be applied, by default, to every {@link Macaroon} being verified
         *
         * @param caveatVerifier - {@link CaveatVerifier} to apply to each {@link Macaroon}
         * @return - {@link MacaroonBakeryBuilder}
         */
        MacaroonBakeryBuilder addDefaultVerifier(CaveatVerifier caveatVerifier) {
            this.caveatVerifiers.add(caveatVerifier);
            return this;
        }

        MacaroonBakeryBuilder withKeyPair(BakeryKeyPair keyPair) {
            this.keyPair = keyPair;
            return this;
        }

        /**
         * Build the {@link MacaroonBakery}
         *
         * @return - {@link MacaroonBakery}
         */
        public MacaroonBakery build() {

            final BakeryKeyPair keys = getKeyPair();

            return new MacaroonBakery(this.serverLocation,
                    this.rootKeyStore,
                    this.thirdPartyKeyStore,
                    keys,
                    this.caveatVerifiers,
                    this.caveatSuppliers);
        }

        private BakeryKeyPair getKeyPair() {
            return Objects.requireNonNullElseGet(this.keyPair, BakeryKeyPair::generate);
        }
    }

    /**
     * Use constant time approach, to compare two byte arrays
     * See also
     * <a href="https://codahale.com/a-lesson-in-timing-attacks">A Lesson In Timing Attacks (or, Don’t use MessageDigest.isEquals)</a>
     *
     * @param a an array
     * @param b an array
     * @return true if both have same length and content
     */
    private static boolean safeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
