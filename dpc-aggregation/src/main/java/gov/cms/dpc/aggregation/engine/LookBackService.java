package gov.cms.dpc.aggregation.engine;

import gov.cms.dpc.aggregation.dao.RosterDAO;
import org.hl7.fhir.dstu3.model.Element;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;

import javax.inject.Inject;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class LookBackService {

    private final RosterDAO rosterDAO;
    private final OperationsConfig operationsConfig;

    @Inject
    public LookBackService(RosterDAO rosterDAO, OperationsConfig operationsConfig) {
        this.rosterDAO = rosterDAO;
        this.operationsConfig = operationsConfig;
    }

    public boolean associatedWithRoster(UUID orgID, String providerID, String patientID) {
        return rosterDAO.withinRoster(orgID, providerID, patientID);
    }

    public boolean hasClaimWithin(ExplanationOfBenefit explanationOfBenefit, UUID organizationID, String providerID, long withinMonth) {
        Date billingPeriod = Optional.ofNullable(explanationOfBenefit).map(ExplanationOfBenefit::getBillablePeriod).map(Period::getEnd).orElse(null);
        return billingPeriod != null
                && getMonthsDifference(billingPeriod, operationsConfig.getLookBackDate()) < withinMonth
                && Optional.of(explanationOfBenefit).map(ExplanationOfBenefit::getProvider).map(Element::getId).orElse("").equals(providerID)
                && Optional.of(explanationOfBenefit).map(ExplanationOfBenefit::getOrganization).map(Element::getId).orElse("").equals(organizationID.toString());
    }

    private long getMonthsDifference(Date date1, Date date2) {
        YearMonth m1 = YearMonth.from(date1.toInstant().atZone(ZoneOffset.UTC));
        YearMonth m2 = YearMonth.from(date2.toInstant().atZone(ZoneOffset.UTC));
        return ChronoUnit.MONTHS.between(m1, m2);
    }
}
