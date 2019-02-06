package gov.cms.dpc.core;

import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Capabilities {

    private Capabilities() {
//        Should not use
    }

    public static CapabilityStatement buildCapabilities() {
        DateTimeType releaseDate = DateTimeType.now();

        CapabilityStatement capabilityStatement = new CapabilityStatement();

        capabilityStatement
                .setStatus(Enumerations.PublicationStatus.ACTIVE)
                .setDateElement(releaseDate)
                .setPublisher("Centers for Medicare and Medicaid Services")
                .setFhirVersion("4.0.0")
                .setSoftware(generateSoftwareComponent(releaseDate))
                .setKind(CapabilityStatement.CapabilityStatementKind.CAPABILITY)
                .setRest(generateRest())
                .setFormat(Arrays.asList(new CodeType("application/json"), new CodeType("application/fhir+json")));

        // Set the narrative
        capabilityStatement.getText().setDivAsString("<div>This is a narrative</div>");
        capabilityStatement.getText().setStatus(Narrative.NarrativeStatus.GENERATED);


        return capabilityStatement;
    }

    private static CapabilityStatement.CapabilityStatementSoftwareComponent generateSoftwareComponent(DateTimeType releaseDate) {
        return new CapabilityStatement.CapabilityStatementSoftwareComponent()
                .setName("Data @ Point of Care API")
                .setVersion("0.0.1")
                .setReleaseDateElement(releaseDate);
    }

    private static List<CapabilityStatement.CapabilityStatementRestComponent> generateRest() {
        final CapabilityStatement.CapabilityStatementRestComponent restComponent = new CapabilityStatement.CapabilityStatementRestComponent();
        restComponent.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

        // Create batch interaction
        final CapabilityStatement.SystemInteractionComponent batchInteraction = new CapabilityStatement.SystemInteractionComponent(new Enumeration<>(new CapabilityStatement.SystemRestfulInteractionEnumFactory(), CapabilityStatement.SystemRestfulInteraction.BATCH));
        restComponent.setInteraction(Collections.singletonList(batchInteraction));
        return Collections.singletonList(restComponent);
    }
}
