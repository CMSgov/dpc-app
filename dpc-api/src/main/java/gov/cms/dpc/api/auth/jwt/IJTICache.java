package gov.cms.dpc.api.auth.jwt;

public interface IJTICache {

    /**
     * Determines whether or not the JWT JTI has been seen in the past time interval.
     * This avoids replay attack vectors.
     *
     * @param jti - {@link String} jti value
     * @return - {@code true} JTI value is OK and has not been used before. {@code false} JTI has been used before
     */
    boolean isJTIOk(String jti);
}
