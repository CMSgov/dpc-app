package gov.cms.dpc.bluebutton.health;

import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Enumerations;
import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * {@link NamedHealthCheck} class that verifies whether or not the Blue Button endpoint is accessible.
 * This simply makes a request to the /metadata endpoint and verifies that the returned {@link CapabilityStatement} has an {@link org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus#ACTIVE} status.
 */
@Singleton
public class BlueButtonHealthCheck extends NamedHealthCheck {

    static final String INVALID_MESSAGE = "BlueButton endpoint returned invalid FHIR Metadata";
    private final BlueButtonClient client;

    @Inject
    public BlueButtonHealthCheck(BlueButtonClient client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        // We can do the simplest sanity check on the capabilities statement, is it active or not?
        try {
            final CapabilityStatement capabilityStatement = client.requestCapabilityStatement();

            if (capabilityStatement.getStatus() == Enumerations.PublicationStatus.ACTIVE) {
                return Result.healthy();
            }
            return Result.unhealthy(INVALID_MESSAGE);
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "blue-button-client";
    }
}
