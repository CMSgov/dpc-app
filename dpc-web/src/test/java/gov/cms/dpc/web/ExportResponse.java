package gov.cms.dpc.web;

import java.util.List;

public class ExportResponse {

    private String transactionTime;
    private String request;
    private List<String> output;
    private String requiresAccessToken;
    private List<FileOutput> error;

    public ExportResponse() {
        // Not used
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public List<String> getOutput() {
        return output;
    }

    public void setOutput(List<String> output) {
        this.output = output;
    }

    public String getRequiresAccessToken() {
        return requiresAccessToken;
    }

    public void setRequiresAccessToken(String requiresAccessToken) {
        this.requiresAccessToken = requiresAccessToken;
    }

    public List<FileOutput> getError() {
        return error;
    }

    public void setError(List<FileOutput> error) {
        this.error = error;
    }

    public static class FileOutput {
        //        private String type;
        private String url;

        public FileOutput() {
//            Not used
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
