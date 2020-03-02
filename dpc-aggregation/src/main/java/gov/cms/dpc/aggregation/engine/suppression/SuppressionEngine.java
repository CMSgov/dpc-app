package gov.cms.dpc.aggregation.engine.suppression;

import io.reactivex.Completable;

public interface SuppressionEngine {
    /**
     * Process a given beneficiary {@link gov.cms.dpc.fhir.DPCIdentifierSystem#MBI} to determine if it should be suppressed or not
     *
     * @param mbi - {@link String} beneficiary MBI
     * @return - {@link Completable} with status of suppression check
     * @throws gov.cms.dpc.aggregation.exceptions.SuppressionException if the beneficiary should be suppressed from the export
     */
    Completable processSuppression(String mbi);
}
