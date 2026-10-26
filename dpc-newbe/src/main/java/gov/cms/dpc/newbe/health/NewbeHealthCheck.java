package gov.cms.dpc.newbe.health;

import gov.cms.dpc.newbe.client.NewbeClient;
import gov.cms.dpc.newbe.models.NewbeHealthCheckResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;
import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck;

@Singleton
public class NewbeHealthCheck extends NamedHealthCheck {

    static final String INVALID_MESSAGE = "BCDA endpoint returned invalid FHIR Metadata";
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    private final NewbeClient client;

    @Inject
    public NewbeHealthCheck(NewbeClient client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        try {
            NewbeHealthCheckResponse response = client.healthy();

            CapabilityStatement capabilityStatement = FHIR_CONTEXT.newJsonParser()
                .parseResource(CapabilityStatement.class, response.body());
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
        return "newbe-client";
    }
}
