package gov.cms.dpc.aggregation.service;

import java.util.List;
import java.util.Optional;

public interface ConsentService {
    Optional<List<ConsentResult>> getConsent(String mbi);
    Optional<List<ConsentResult>> getConsent(List<String> mbis);
}
