package gov.cms.dpc.common.health;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.google.inject.name.Named;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import ru.vyarus.dropwizard.guice.module.installer.feature.health.NamedHealthCheck;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Healthcheck for use with FHIR based services.  It pings the local metadata end point and makes sure it returns a 200.
 * This doesn't require auth, and should always be successful if the service is able to respond to FHIR requests.
 */
@Singleton
public class FhirMetaDataHealthCheck extends NamedHealthCheck {
	private IGenericClient localFhirClient;

	@Inject
	public FhirMetaDataHealthCheck(@Named("localFhirClient") IGenericClient localFhirClient) {
		this.localFhirClient = localFhirClient;
	}
	@Override
	public String getName() {
		return "fhir_meta_data";
	}

	@Override
	public Result check() {
		try {
			// Don't need the return value.  It will throw an exception on anything but a 200.
			localFhirClient.capabilities().ofType(CapabilityStatement.class).execute();
			return Result.healthy();
		}
		catch(Exception e) {
			return Result.unhealthy("FhirMetaDataHealthCheck: " + e.getMessage());
		}
	}
}
