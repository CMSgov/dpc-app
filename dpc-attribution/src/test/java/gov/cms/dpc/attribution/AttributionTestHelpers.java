package gov.cms.dpc.attribution;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import org.hl7.fhir.dstu3.model.Practitioner;

public class AttributionTestHelpers {

    public static Practitioner createPractitionerResource(String NPI) {
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue(NPI);
        practitioner.addName()
                .setFamily("Practitioner").addGiven("Test");

        return practitioner;
    }

    public static IGenericClient createFHIRClient(FhirContext ctx, String serverURL) {
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return ctx.newRestfulGenericClient(serverURL);
    }
}
