package gov.cms.dpc.api.health;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.codahale.metrics.health.HealthCheck;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Simple check for validating that the Attribution Service is healthy
 */
@Singleton
public class AttributionHealthCheck extends HealthCheck {

    private final IGenericClient client;

    @Inject
    public AttributionHealthCheck(IGenericClient client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        try {
            this
                    .client
                    .capabilities()
                    .ofType(CapabilityStatement.class)
                    .encodedJson()
                    .execute();
            return Result.healthy();
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }
}
