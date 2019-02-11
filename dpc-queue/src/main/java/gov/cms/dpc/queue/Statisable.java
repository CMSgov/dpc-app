package gov.cms.dpc.queue;

public interface Statisable {
    JobStatus getStatus();
    void setStatus(JobStatus status);
}
