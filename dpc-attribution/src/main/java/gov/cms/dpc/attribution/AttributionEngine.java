package gov.cms.dpc.attribution;

import java.util.List;

public interface AttributionEngine {

    List<String> getAttributedPatients(String providerID);
}
