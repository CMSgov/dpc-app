package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.MacaroonsConstants;

import java.util.Arrays;
import java.util.Objects;

/**
 * Defines the data necessary to generate a Macaroon caveat.
 * This will always include a Key (e.g. user_id), an Operation (e.g '=') and a Value (e.g. user123).
 * It may optionally include a location (defined as any non-empty string value) which makes it a third-party caveat
 * <p>
 * The underlying string format is \"{caveat key} {operator} {caveat value}\"
 */
public class MacaroonCaveat {


    private String location;
    private byte[] rawCaveat;
    private byte[] verificationID;

    public MacaroonCaveat() {
        this.location = "";
        this.verificationID = new byte[0];
    }

    public MacaroonCaveat(MacaroonCondition condition) {
        this.location = "";
        this.rawCaveat = condition.toBytes();
        this.verificationID = new byte[0];
    }

    public MacaroonCaveat(String location, MacaroonCondition condition) {
        this.location = location;
        this.rawCaveat = condition.toBytes();
        this.verificationID = new byte[0];
    }

    /**
     * Create a caveat which may have a location (thus making it a third-party caveat)
     *
     * @param location  - {@link String} third-party caveat location
     * @param rawCaveat - {@link Byte} raw caveat bytes which usually represents an encrypted third-party caveat
     */
    public MacaroonCaveat(String location, byte[] rawCaveat) {
        this.location = location;
        this.verificationID = new byte[0];
        this.rawCaveat = rawCaveat;

    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public byte[] getRawCaveat() {
        return rawCaveat;
    }

    public void setRawCaveat(byte[] rawCaveat) {
        this.rawCaveat = rawCaveat;
    }

    public void setVerificationID(byte[] verificationID) {
        this.verificationID = verificationID;
    }

    /**
     * Get the verification ID from the caveat.
     * This is only for third-party caveats, so {@link MacaroonCaveat#isThirdParty()} must be true.
     *
     * @return - {@link Byte} of verification ID
     */
    public byte[] getVerificationID() {
        return verificationID;
    }

    /**
     * Determines if the given Caveat is a Third-party caveat.
     * If the {@link MacaroonCaveat#getLocation()} returns a non-empty value, then the Caveat is a third-party caveat
     *
     * @return - {@code true} Caveat is a third-party caveat. {@code false} caveat is a first-party caveat.
     */
    public boolean isThirdParty() {
        return !location.equals("");
    }

    public MacaroonCondition getCondition() {
        return MacaroonCondition.parseFromString(new String(this.rawCaveat, MacaroonsConstants.IDENTIFIER_CHARSET));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MacaroonCaveat)) return false;
        MacaroonCaveat that = (MacaroonCaveat) o;
        return Objects.equals(location, that.location) &&
                Arrays.equals(rawCaveat, that.rawCaveat) &&
                Arrays.equals(verificationID, that.verificationID);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(location);
        result = 31 * result + Arrays.hashCode(rawCaveat);
        result = 31 * result + Arrays.hashCode(verificationID);
        return result;
    }

    @Override
    public String toString() {
        return new String(this.rawCaveat, MacaroonsConstants.RAW_BYTE_CHARSET);
    }
}
