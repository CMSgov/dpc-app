package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.CaveatPacket;
import com.github.nitram509.jmacaroons.MacaroonsConstants;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacaroonCondition {
    // Regex for matching key, op and value from a given caveat string
    private static final Pattern caveatPattern = Pattern.compile("([a-zA-Z0-9_]*)\\s([=><!]{1,2})\\s(.*)");

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
    public MacaroonCondition(String key, Operator op, String value) {
        this.key = key;
        this.op = op;
        this.value = value;
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
     * @return - {@link Operator}
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

    public byte[] toBytes() {
        return toString().getBytes(MacaroonsConstants.IDENTIFIER_CHARSET);
    }

    /**
     * Format the caveat value into the Macaroons caveat ID format, which is ('key'\s'op'\s'value').
     * Notice the spaces separating each
     *
     * @return - {@link String} formatted caveat id
     */
    @Override
    public String toString() {
        return String.format("%s %s %s", this.getKey(), this.getOp().getOp(), this.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MacaroonCondition)) return false;
        MacaroonCondition that = (MacaroonCondition) o;
        return Objects.equals(key, that.key) &&
                Objects.equals(value, that.value) &&
                op == that.op;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, op);
    }

    static MacaroonCondition parseFromPacket(CaveatPacket packet) {
        final String packetValue = packet.getValueAsText();
        return parseFromString(packetValue);
    }

    static MacaroonCondition parseFromString(String caveatValue) {
        final Matcher matcher = caveatPattern.matcher(caveatValue);
        if (!matcher.matches() || matcher.groupCount() != 3) {
            throw new IllegalArgumentException(String.format("Cannot parse caveat: %s", caveatValue));
        }
        return new MacaroonCondition(matcher.group(1), Operator.fromString(matcher.group(2)), matcher.group(3));
    }
}
