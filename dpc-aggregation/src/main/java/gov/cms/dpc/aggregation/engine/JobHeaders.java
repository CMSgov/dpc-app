package gov.cms.dpc.aggregation.engine;

import com.google.common.net.HttpHeaders;
import gov.cms.dpc.common.Constants;
import gov.cms.dpc.queue.models.JobQueueBatch;

import java.util.HashMap;
import java.util.Map;

public class JobHeaders {

    public JobQueueBatch getJob() {
        return job;
    }

    private JobQueueBatch job;

    public JobHeaders(JobQueueBatch job) {
        this.job = job;
    }

    public  Map<String, String> fetchHeaders() {
        return buildHeaders(getJob());
    }
    public Map<String, String> buildHeaders(JobQueueBatch job) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.X_FORWARDED_FOR, job.getRequestingIP());
        headers.put(Constants.BlueButton.ORIGINAL_QUERY_ID_HEADER, job.getJobID().toString());  //test this
        if (job.isBulk()) {
            headers.put(Constants.BULK_JOB_ID_HEADER, job.getJobID().toString());
            headers.put(Constants.BULK_CLIENT_ID_HEADER, job.getProviderNPI()); //test this
            headers.put(Constants.BlueButton.BULK_CLIENTNAME_HEADER,Constants.BlueButton.APPLICATION_NAME_DESC); //test this
        } else {
            headers.put(Constants.DPC_CLIENT_ID_HEADER, job.getProviderNPI()); //test this
            headers.put(Constants.BlueButton.APPLICATION_NAME_HEADER,Constants.BlueButton.APPLICATION_NAME_DESC); //test this
            headers.put(Constants.BlueButton.ORIGINAL_QUERY_TIME_STAMP_HEADER,job.getTransactionTime().toString()); //test this
        }
        return headers;
    }
}
