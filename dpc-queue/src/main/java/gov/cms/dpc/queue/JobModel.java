package gov.cms.dpc.queue;

class JobModel<T> {

    private final T data;
    private JobStatus status;

    JobModel(JobStatus initialStatus, T data) {
        this.data = data;
        this.status = initialStatus;
    }

    T getData() {
        return data;
    }

    JobStatus getStatus() {
        return status;
    }

    void setStatus(JobStatus status) {
        this.status = status;
    }
}
