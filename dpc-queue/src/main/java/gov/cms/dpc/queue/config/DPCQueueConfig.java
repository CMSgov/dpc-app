package gov.cms.dpc.queue.config;

public interface DPCQueueConfig {
    int getPollingFrequency();

    DPCAwsQueueConfiguration getDpcAwsQueueConfiguration();
}
