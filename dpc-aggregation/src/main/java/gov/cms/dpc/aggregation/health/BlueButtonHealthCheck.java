package gov.cms.dpc.aggregation.health;

import com.codahale.metrics.health.HealthCheck;
import gov.cms.dpc.aggregation.bbclient.BlueButtonClient;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Enumerations;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * {@link HealthCheck} class that verifies whether or not the Blue Button endpoint is accessible.
 * This simply makes a request to the /metadata endpoint and verifies that the returned {@link CapabilityStatement} has an {@link org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus#ACTIVE} status.
 */
@Singleton
public class BlueButtonHealthCheck extends HealthCheck {

    private final BlueButtonClient client;

    @Inject
    public BlueButtonHealthCheck(BlueButtonClient client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        // We can do the simplest sanity check on the capabilities statement, is it active or not?
        final CapabilityStatement capabilityStatement = client.requestCapabilityStatement();

        if (capabilityStatement.getStatus() == Enumerations.PublicationStatus.ACTIVE) {
            return Result.healthy();
        }
        return Result.unhealthy("BlueButton endpoint returned invalid FHIR Metadata");
    }
}
