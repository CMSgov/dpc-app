package gov.cms.dpc.fhir;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;

public enum DPCResourceType {
    Bundle,
    Coverage,
    ExplanationOfBenefit,
    Group,
    OperationOutcome,
    Organization,
    Patient,
    Practitioner,
    Schedule;


    public String getPath() {
        return switch (this) {
            case Bundle -> "bundle";
            case Coverage -> "coverage";
            case ExplanationOfBenefit -> "explanationofbenefit";
            case Group -> "group";
            case OperationOutcome -> "operationoutcome";
            case Organization -> "organization";
            case Patient -> "patient";
            case Practitioner -> "practitioner";
            case Schedule -> "schedule";
        };
    }


    public static DPCResourceType fromCode(String code) throws FHIRException {
        return fromPath(code.toLowerCase());
    }

    public static DPCResourceType fromPath(String path) throws FHIRException {
        return switch (path) {
            case "bundle" -> Bundle;
            case "coverage" -> Coverage;
            case "explanationofbenefit" -> ExplanationOfBenefit;
            case "group" -> Group;
            case "operationoutcome" -> OperationOutcome;
            case "organization" -> Organization;
            case "patient" -> Patient;
            case "practitioner" -> Practitioner;
            case "schedule" -> Schedule;
            default -> throw new FHIRException("Unknown resource type: " + path);
        };
    }

    public static Class<? extends Resource> toResource(DPCResourceType resourceType) {
        return switch(resourceType) {
            case Bundle -> Bundle.class;
            case Coverage -> Coverage.class;
            case ExplanationOfBenefit -> ExplanationOfBenefit.class;
            case Group -> Group.class;
            case OperationOutcome -> OperationOutcome.class;
            case Organization -> Organization.class;
            case Patient -> Patient.class;
            case Practitioner -> Practitioner.class;
            case Schedule -> Schedule.class;
        };
    }

}
