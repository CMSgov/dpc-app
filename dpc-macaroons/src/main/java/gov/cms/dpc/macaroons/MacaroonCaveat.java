package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.CaveatPacket;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines the data necessary to generate a Macaroon caveat.
 * This will always include a Key (e.g. user_id), an Operation (e.g '=') and a Value (e.g. user123).
 * It may optionally include a location (defined as any non-empty string value) which makes it a third-party caveat
 */
public class MacaroonCaveat {

    // Regex for matching key, op and value from a given caveat string
    private static final Pattern caveatPattern = Pattern.compile("([a-zA-Z0-9_]*)\\s([=><!]{1,2})\\s([a-zA-Z0-9_]*[^\\s])");

    public enum Operator {
        /**
         * Equality between the caveat value and the verifier value
         */
        EQ("="),
        /**
         * Inequality between the caveat value and the verifier value
         */
        NEQ("!="),
        /**
         * Verifier value is less than the caveat value
         */
        LT("<"),
        /**
         * Verifier value is less than or equal to the caveat value
         */
        LEQ("<="),
        /**
         * Verifier value is greater than the caveat value
         */
        GT(">"),
        /**
         * Verifier value is greater than or equal to øthe caveat value
         */
        GEQ(">=");

        private final String op;

        Operator(String op) {
            this.op = op;
        }

        public String getOp() {
            return op;
        }

        public static Operator fromString(String opString) {
            for (final Operator op : Operator.values()) {
                if (op.getOp().equalsIgnoreCase(opString)) {
                    return op;
                }
            }
            throw new IllegalArgumentException(String.format("Cannot determine Operation enum from: %s", opString));
        }


    }

    private final String location;
    private final String key;
    private final String value;
    private final Operator op;

    /**
     * Create a first-party caveat (e.g. one that does not have a location)
     *
     * @param key   -{@link String} Caveat key
     * @param op    - {@link Operator} Caveat operator
     * @param value - {@link String Caveat value}
     */
    public MacaroonCaveat(String key, Operator op, String value) {
        this.location = "";
        this.key = key;
        this.op = op;
        this.value = value;
    }

    /**
     * Create a caveat which may have a location (thus making it a third-party caveat)
     *
     * @param location - {@link String} third-party caveat location
     * @param key      -{@link String} Caveat key
     * @param op       - {@link Operator} Caveat operator
     * @param value    - {@link String Caveat value}
     */
    public MacaroonCaveat(String location, String key, Operator op, String value) {
        this.location = location;
        this.key = key;
        this.op = op;
        this.value = value;

    }

    public String getLocation() {
        return location;
    }

    /**
     * Get the caveat key
     *
     * @return - {@link String} caveat key
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the caveat comparison operator
     *
     * @return
     */
    public Operator getOp() {
        return op;
    }

    /**
     * Get the caveat value
     *
     * @return - {@link String} caveat value
     */
    public String getValue() {
        return value;
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

    /**
     * Format the caveat value into the Macaroons caveat ID format, which is ('key'\s'op'\s'value').
     * Notice the spaces separating each
     *
     * @return - {@link String} formatted caveat id
     */
    public String getCaveatText() {
        return String.format("%s %s %s", this.getKey(), this.getOp().getOp(), this.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacaroonCaveat that = (MacaroonCaveat) o;
        return location.equals(that.location) &&
                key.equals(that.key) &&
                value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, key, value);
    }

    static MacaroonCaveat parseFromPacket(CaveatPacket packet) {
        final String packetValue = packet.getValueAsText();
        final Matcher matcher = caveatPattern.matcher(packetValue);
        if (!matcher.matches() || matcher.groupCount() != 3) {
            throw new IllegalArgumentException(String.format("Cannot parse caveat: %s", packetValue));
        }
        return new MacaroonCaveat(matcher.group(1), Operator.fromString(matcher.group(2)), matcher.group(3));
    }
}
