package gov.cms.dpc.queue.models;

import gov.cms.dpc.fhir.DPCResourceType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ExternalJobQueueBatch(
        UUID jobID,
        UUID orgID,
        String orgNPI,
        String providerNPI,
        List<String> mbis,
        List<DPCResourceType> resourceTypes,
        OffsetDateTime since,
        OffsetDateTime transactionTime,
        String requestingIP,
        String requestUrl,
        UUID groupID,
        boolean isBulk,
        boolean isSmoke) {

    public static class Builder {
        private UUID jobID;
        private UUID orgID;
        private String orgNPI;
        private String providerNPI;
        private List<String> mbis;
        private List<DPCResourceType> resourceTypes;
        private OffsetDateTime since;
        private OffsetDateTime transactionTime;
        private String requestingIP;
        private String requestUrl;
        private UUID groupID;
        private boolean isBulk;
        private boolean isSmoke;

        public Builder jobID(UUID jobID) {
            this.jobID = jobID;
            return this;
        }

        public Builder orgID(UUID orgID) {
            this.orgID = orgID;
            return this;
        }

        public Builder orgNPI(String orgNPI) {
            this.orgNPI = orgNPI;
            return this;
        }

        public Builder providerNPI(String providerNPI) {
            this.providerNPI = providerNPI;
            return this;
        }

        public Builder mbis(List<String> mbis) {
            this.mbis = mbis;
            return this;
        }

        public Builder resourceTypes(List<DPCResourceType> resourceTypes) {
            this.resourceTypes = resourceTypes;
            return this;
        }

        public Builder since(OffsetDateTime since) {
            this.since = since;
            return this;
        }

        public Builder transactionTime(OffsetDateTime transactionTime) {
            this.transactionTime = transactionTime;
            return this;
        }

        public Builder requestingIP(String requestingIP) {
            this.requestingIP = requestingIP;
            return this;
        }

        public Builder requestUrl(String requestUrl) {
            this.requestUrl = requestUrl;
            return this;
        }

        public Builder groupID(UUID groupID) {
            this.groupID = groupID;
            return this;
        }

        public Builder isBulk(boolean isBulk) {
            this.isBulk = isBulk;
            return this;
        }

        public Builder isSmoke(boolean isSmoke) {
            this.isSmoke = isSmoke;
            return this;
        }

        public ExternalJobQueueBatch build() {
            return new ExternalJobQueueBatch(jobID, orgID, orgNPI, providerNPI, mbis, resourceTypes,
                    since, transactionTime, requestingIP, requestUrl, groupID, isBulk, isSmoke);
        }
    }
}
