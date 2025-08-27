package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.aggregation.jdbi.ConsentDAO;

import java.util.List;
import java.util.Optional;

public interface ConsentService {
    Optional<List<ConsentResult>> getConsent(String mbi);
    Optional<List<ConsentResult>> getConsent(List<String> mbis);
    ConsentDAO getConsentDAO();
}
