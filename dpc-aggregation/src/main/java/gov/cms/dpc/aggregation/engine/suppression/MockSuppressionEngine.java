package gov.cms.dpc.aggregation.engine.suppression;

import gov.cms.dpc.aggregation.exceptions.SuppressionException;
import io.reactivex.Completable;

public class MockSuppressionEngine implements SuppressionEngine {

    public static final String PATIENT_OPT_OUT = "1SQ3F00AA00";

    public MockSuppressionEngine() {
        // Not used
    }

    @Override
    public Completable processSuppression(String mbi) {
        if (mbi.equals(PATIENT_OPT_OUT)) {
            return Completable.error(() -> new SuppressionException(SuppressionException.SuppressionReason.OPT_OUT, mbi, "Opted-out"));
        }
        return Completable.complete();
    }
}
