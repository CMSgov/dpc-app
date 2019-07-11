package gov.cms.dpc.fhir.validations.profiles;

/**
 * Profile loader interface used by {@link java.util.ServiceLoader} in order to discover specific profiles to use.
 * <p>
 * This simply points to the location of the profile JSON description in the JAR resources
 */
public interface IProfileLoader {
    /**
     * Path where corresponding profile JSON is located.
     *
     * @return - {@link String} resource path
     */
    String getPath();

    /**
     * Profile URI
     *
     * @return - {@link String} representation of profile URI
     */
    String getURI();
}
