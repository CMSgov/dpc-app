package gov.cms.dpc.attribution;

import javax.inject.Inject;
import java.util.List;

/**
 * In-memory attribution engine, mostly designed for testing
 */
public class LocalAttributionEngine implements AttributionEngine {

    @Inject
    LocalAttributionEngine() {

    }

    @Override
    public List<String> getAttributedPatients(String providerID) {
        return null;
    }
}
