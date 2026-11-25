package gov.cms.dpc.fhir.validations.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import gov.cms.dpc.fhir.validations.profiles.r4.AddressProfile;
import gov.cms.dpc.fhir.validations.profiles.r4.AttestationProfile;
import gov.cms.dpc.fhir.validations.profiles.r4.AttributionRosterProfile;
import gov.cms.dpc.fhir.validations.profiles.r4.OrganizationProfile;
import gov.cms.dpc.fhir.validations.profiles.r4.PatientProfile;
import gov.cms.dpc.fhir.validations.profiles.r4.PractitionerProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.conformance.ProfileUtilities;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.StructureDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DPCProfileSupport extends PrePopulatedValidationSupport {

    @Inject
    public DPCProfileSupport(@Named("r4") FhirContext context) {
        super(context);
        final DefaultProfileValidationSupport defaults = new DefaultProfileValidationSupport(context);
        final ProfileUtilities utilities = new ProfileUtilities(new HapiWorkerContext(context, defaults), new ArrayList<>(), null);
        // Register profiles before the definitions that reference them.
        for (String path : List.of(
                new AddressProfile().getPath(),
                new OrganizationProfile().getPath(),
                new PractitionerProfile().getPath(),
                new PatientProfile().getPath(),
                new AttributionRosterProfile().getPath(),
                new AttestationProfile().getPath())) {
            try (InputStream stream = DPCProfileSupport.class.getClassLoader().getResourceAsStream(path)) {
                if (stream == null) {
                    throw new IllegalStateException("Cannot load R4 profile: " + path);
                }
                final StructureDefinition definition = context.newJsonParser().parseResource(StructureDefinition.class, stream);
                final StructureDefinition base = (StructureDefinition) defaults.fetchStructureDefinition(definition.getBaseDefinition());
                if (base == null) {
                    throw new IllegalStateException("Cannot load R4 base profile: " + definition.getBaseDefinition());
                }
                utilities.generateSnapshot(base, definition, "", "", "");
                if (!definition.hasSnapshot() || definition.getSnapshot().getElement().isEmpty()) {
                    throw new IllegalStateException("Cannot generate R4 profile snapshot: " + path);
                }
                addStructureDefinition(definition);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read R4 profile: " + path, e);
            }
        }
    }
}
