package gov.cms.dpc.attribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * In-memory attribution engine, mostly designed for testing
 */
public class LocalAttributionEngine implements AttributionEngine {

    private static final Logger logger = LoggerFactory.getLogger(LocalAttributionEngine.class);

    private final Map<String, Set<String>> attributionMap;

    @Inject
    LocalAttributionEngine() {
        this.attributionMap = new HashMap<>();
    }

    @Override
    public synchronized Set<String> getAttributedBeneficiaries(String providerID) {
        return Objects.requireNonNullElseGet(
                this.attributionMap.get(providerID),
                HashSet::new);
    }

    @Override
    public synchronized void addAttributionRelationship(String providerID, String beneficiaryID) {
        Set<String> attributedBeneficiaries = this.attributionMap.get(providerID);
        // If there aren't any attributed patients, create a new set and associate it
        // Otherwise, add the beneficiary to the provider
        if (attributedBeneficiaries == null) {
            attributedBeneficiaries = new HashSet<>();
        }

        attributedBeneficiaries.add(beneficiaryID);

        this.attributionMap.put(providerID, attributedBeneficiaries);
    }

    @Override
    public synchronized void removeAttributionRelationship(String providerID, String beneficiaryID) {
        final Set<String> attributedBeneficiaries = this.attributionMap.get(providerID);
        if (attributedBeneficiaries == null) {
            return;
        }

        final boolean removed = attributedBeneficiaries.remove(beneficiaryID);
        logger.debug("Removed {} from {}? {}", beneficiaryID, providerID, removed);
        this.attributionMap.put(beneficiaryID, attributedBeneficiaries);
    }
}
