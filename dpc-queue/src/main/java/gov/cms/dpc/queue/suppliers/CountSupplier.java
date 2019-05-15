package gov.cms.dpc.queue.suppliers;

import gov.cms.dpc.queue.models.JobResult;

/**
 * Supplier pattern for either JobResult::getCount or JobResult::getErrorCount
 */
@FunctionalInterface
public interface CountSupplier {
    int getCount(JobResult result);
}
