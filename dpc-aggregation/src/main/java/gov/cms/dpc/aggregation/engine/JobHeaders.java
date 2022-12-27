package gov.cms.dpc.aggregation.engine;

import com.google.common.net.HttpHeaders;
import gov.cms.dpc.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class JobHeaders {

    private final String requestingIP;
    private final String jobID;
    private final String providerNPI;
    private final String transactionTime;
    private final boolean isBulk;

    private static final Logger logger = LoggerFactory.getLogger(JobHeaders.class);

    public JobHeaders(String requestingIP, String jobID, String providerNPI, String transactionTime, boolean isBulk) {
        this.requestingIP=requestingIP;
        this.jobID=jobID;
        this.providerNPI=providerNPI;
        this.transactionTime=transactionTime;
        this.isBulk=isBulk;
    }

    public Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.X_FORWARDED_FOR, this.requestingIP);
        headers.put(Constants.BlueButton.ORIGINAL_QUERY_ID_HEADER, this.jobID);
        if (this.isBulk) {
            headers.put(Constants.BULK_JOB_ID_HEADER, this.jobID);
            headers.put(Constants.BULK_CLIENT_ID_HEADER, this.providerNPI);
            headers.put(Constants.BlueButton.BULK_CLIENTNAME_HEADER,Constants.BlueButton.APPLICATION_NAME_DESC);
            logger.info("adding blue button BFD headers for bulk job: jobID, providerNPI, appName" + this.jobID, " ",
                    this.providerNPI, " ", Constants.BlueButton.APPLICATION_NAME_DESC);
        } else {
            headers.put(Constants.DPC_CLIENT_ID_HEADER, this.providerNPI);
            headers.put(Constants.BlueButton.APPLICATION_NAME_HEADER,Constants.BlueButton.APPLICATION_NAME_DESC);
            headers.put(Constants.BlueButton.ORIGINAL_QUERY_TIME_STAMP_HEADER,this.transactionTime);
            logger.info("adding blue button BFD headers for non-bulk job: jobID, providerNPI, appName" +
                    this.providerNPI, " ", Constants.BlueButton.APPLICATION_NAME_DESC, " ", this.transactionTime);
        }
        return headers;
    }

}
