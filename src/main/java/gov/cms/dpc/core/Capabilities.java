package gov.cms.dpc.core;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;

import java.util.Arrays;

public class Capabilities {

    private Capabilities() {
//        Should not use
    }

    public static CapabilityStatement buildCapabilities() {
        DateTimeType releaseDate = DateTimeType.now();

        CapabilityStatement.CapabilityStatementSoftwareComponent softwareElement = new CapabilityStatement.CapabilityStatementSoftwareComponent()
                .setName("Data @ Point of Care API")
                .setVersion("0.0.1")
                .setReleaseDateElement(releaseDate);

        CapabilityStatement capabilityStatement = new CapabilityStatement();

        capabilityStatement
                .setStatus(Enumerations.PublicationStatus.ACTIVE)
                .setDateElement(releaseDate)
                .setPublisher("Centers for Medicare and Medicaid Services")
                .setFhirVersion("4.0.0")
                .setSoftware(softwareElement)
                .setFormat(Arrays.asList(new CodeType("application/json"), new CodeType("application/fhir+json")));

        return capabilityStatement;
    }
}
