package gov.cms.dpc.api.core;


import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.common.utils.PropertiesProvider;
import gov.cms.dpc.fhir.FHIRFormatters;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;

import static org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementSoftwareComponent;
import static org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementImplementationComponent;

public class Capabilities {

    private static final Logger logger = LoggerFactory.getLogger(Capabilities.class);
    private static final String CAP_STATEMENT = "DPCCapabilities.json";

    private static final Object lock = new Object();
    private static volatile CapabilityStatement statement;

    private Capabilities() {
    }

    /**
     * Get the system's {@link CapabilityStatement}.
     * <p>
     * This value is lazily generated the first time it's called.
     *
     * @param baseURL the base URL of the service
     * @return - {@link CapabilityStatement} of system.
     */
    public static CapabilityStatement getCapabilities(String baseURL) {
        // Double lock check to lazy init capabilities statement
        if (statement == null) {
            synchronized (lock) {
                if (statement == null) {
                    logger.debug("Building capabilities statement");
                    statement = buildCapabilities(baseURL);
                    return statement;
                }
            }
        }
        logger.trace("Returning cached capabilities statement");
        return statement;
    }


    private static CapabilityStatement buildCapabilities(String baseURL) {
        final PropertiesProvider pp = new PropertiesProvider();

        DateTimeType releaseDate = DateTimeType.parseV3(pp.getBuildTimestamp().format(FHIRFormatters.DATE_TIME_FORMATTER));

        logger.debug("Reading {} from resources", CAP_STATEMENT);
        try (InputStream resource = Capabilities.class.getClassLoader().getResourceAsStream(CAP_STATEMENT)) {
            if (resource == null) {
                throw new MissingResourceException("Cannot find Capability Statement", Capabilities.class.getName(), CAP_STATEMENT);
            }

            final CapabilityStatement capabilityStatement = FhirContext.forDstu3().newJsonParser().parseResource(CapabilityStatement.class, resource);
            return capabilityStatement
                    .setVersion(pp.getApplicationVersion())
                    .setImplementation(generateImplementationComponent(baseURL))
                    .setSoftware(generateSoftwareComponent(releaseDate, pp.getApplicationVersion()));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read capability statement", e);
        }
    }

    private static CapabilityStatementSoftwareComponent generateSoftwareComponent(DateTimeType releaseDate, String releaseVersion) {
        return new CapabilityStatementSoftwareComponent()
                .setName("Data @ Point of Care API")
                .setVersion(releaseVersion)
                .setReleaseDateElement(releaseDate);
    }

    private static CapabilityStatementImplementationComponent generateImplementationComponent(String baseURL) {

        return new CapabilityStatementImplementationComponent()
                .setDescription("As patients move throughout the healthcare system, providers often struggle to gain and maintain a complete picture of their medical history. Data at the Point of Care fills in the gaps with claims data to inform providers with secure, structured patient history, past procedures, medication adherence, and more.")
                .setUrl(baseURL);
    }
}
