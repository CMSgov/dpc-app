package gov.cms.dpc.fhir;

import org.hl7.fhir.exceptions.FHIRException;

public enum DPCResourceType {
    Bundle,
    Coverage,
    Endpoint,
    ExplanationOfBenefit,
    Group,
    OperationOutcome,
    Organization,
    Patient,
    Practitioner,
    Schedule;


    public String getPath() {
        switch (this) {
            case Bundle:
                return "bundle";
            case Coverage:
                return "coverage";
            case Endpoint:
                return "endpoint";
            case ExplanationOfBenefit:
                return "explanationofbenefit";
            case Group:
                return "group";
            case OperationOutcome:
                return "operationoutcome";
            case Organization:
                return "organization";
            case Patient:
                return "patient";
            case Practitioner:
                return "practitioner";
            case Schedule:
                return "schedule";
        }
        return null;
    }


    public static DPCResourceType fromCode(String code) throws FHIRException {
        if ("Bundle".equals(code))
            return Bundle;
        if ("Coverage".equals(code))
            return Coverage;
        if ("Endpoint".equals(code))
            return Endpoint;
        if ("ExplanationOfBenefit".equals(code))
            return ExplanationOfBenefit;
        if ("Group".equals(code))
            return Group;
        if ("OperationOutcome".equals(code))
            return OperationOutcome;
        if ("Organization".equals(code))
            return Organization;
        if ("Patient".equals(code))
            return Patient;
        if ("Practitioner".equals(code))
            return Practitioner;
        if ("Schedule".equals(code))
            return Schedule;

        throw new FHIRException("Unknown resource type"+code);
    }

}
