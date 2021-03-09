package gov.cms.dpc.aggregation.engine;

public enum OutcomeReason {
    LOOK_BACK_NO_DATA("DPC couldn't find any claims for this MBI; unable to demonstrate relationship with provider or organization"),
    LOOK_BACK_NO_NPI_MATCH("DPC couldn't find a claim for this MBI from an NPI in this organization"),
    LOOK_BACK_NO_DATE_MATCH("DPC couldn't find a claim within the past 18 months for this MBI from an NPI in this organization"),
    CONSENT_OPTED_OUT("Data not available for opted out patient"),
    INTERNAL_ERROR("Unable to retrieve patient data due to internal error");

    public final String detail;

    OutcomeReason(String detail) {
        this.detail = detail;
    }
}
