package gov.cms.dpc.common;
public final class Constants {
    private Constants() {}
    public static final String INCLUDE_IDENTIFIERS_HEADER = "IncludeIdentifiers";
    public static final String BULK_CLIENT_ID_HEADER = "BULK-CLIENTID";
    public static final String BULK_JOB_ID_HEADER = "BULK-JOBID";
    public static final String DPC_CLIENT_ID_HEADER = "DPC_CLIENTID";

    public static final String BFD_ORIGINAL_QUERY_ID_HEADER = "BlueButton-OriginalQueryId";  //TODO: REMOVE ME
    public static final String DPC_REQUEST_ID_HEADER = "X-Request-Id";
    public interface BlueButton {
        public static final String ORIGINAL_QUERY_ID_HEADER = "BlueButton-OriginalQueryId";
        public static final String ORIGINAL_QUERY_TIME_STAMP_HEADER = "BlueButton-OriginalQueryTimestamp";
        public static final String BULK_CLIENTNAME_HEADER = "BULK-CLIENTNAME";
        public static final String APPLICATION_NAME_HEADER = "BlueButton-Application";
        public static final String APPLICATION_ID_HEADER = "BlueButton-ApplicationId";
        public static final String APPLICATION_NAME_DESC = "DPC";
    }
}