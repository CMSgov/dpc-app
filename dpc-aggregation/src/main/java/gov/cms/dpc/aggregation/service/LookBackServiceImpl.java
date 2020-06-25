package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.aggregation.dao.RosterDAO;
import gov.cms.dpc.aggregation.engine.OperationsConfig;
import io.dropwizard.hibernate.UnitOfWork;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Element;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;

import javax.inject.Inject;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class LookBackServiceImpl implements LookBackService {

    private final RosterDAO rosterDAO;
    private final OperationsConfig operationsConfig;

    @Inject
    public LookBackServiceImpl(RosterDAO rosterDAO, OperationsConfig operationsConfig) {
        this.rosterDAO = rosterDAO;
        this.operationsConfig = operationsConfig;
    }

    @Override
    @UnitOfWork
    public String getProviderNPIFromRoster(UUID orgUUID, String providerOrRosterID, String patientMBI) {
        //Expect only one roster for the parameters, otherwise return null
        return Try.of(() -> rosterDAO.retrieveProviderNPIFromRoster(orgUUID, UUID.fromString(providerOrRosterID), patientMBI)).getOrNull();
    }

    @Override
    public boolean hasClaimWithin(ExplanationOfBenefit explanationOfBenefit, UUID organizationUUID, String providerUUID, long withinMonth) {
        Optional<Date> billingPeriod = Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getBillablePeriod)
                .map(Period::getEnd);

        Optional<String> providerID = Optional.ofNullable(providerUUID);

        Optional<String> organizationID = Optional.ofNullable(organizationUUID)
                .map(UUID::toString);

        Optional<String> eobOrganizationID = Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getOrganization)
                .map(Element::getId);

        List<String> eobProviderNPIs = extractPractionerNPIs(explanationOfBenefit);

        return billingPeriod.isPresent()
                && providerID.isPresent()
                && organizationID.isPresent() && eobOrganizationID.isPresent()
                && getMonthsDifference(billingPeriod.get(), operationsConfig.getLookBackDate()) < withinMonth
                && eobProviderNPIs.contains(providerID.get())
                && organizationID.get().equals(eobOrganizationID.get());
    }

    private List<String> extractPractionerNPIs(ExplanationOfBenefit explanationOfBenefit) {
        List<String> eobProviderNPIs = new ArrayList<>();
        Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getProvider)
                .map(Element::getId)
                .filter(StringUtils::isNotBlank)
                .ifPresent(eobProviderNPIs::add);

        Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getCareTeam)
                .ifPresent(careTeamComponents -> {
                    careTeamComponents.stream()
                            .filter(ExplanationOfBenefit.CareTeamComponent::hasProvider)
                            .map(ExplanationOfBenefit.CareTeamComponent::getProvider)
                            .map(Reference::getId)
                            .filter(StringUtils::isNotBlank)
                            .forEach(eobProviderNPIs::add);
                });
        return eobProviderNPIs;
    }

    private long getMonthsDifference(Date date1, Date date2) {
        YearMonth m1 = YearMonth.from(date1.toInstant().atZone(ZoneOffset.UTC));
        YearMonth m2 = YearMonth.from(date2.toInstant().atZone(ZoneOffset.UTC));
        return ChronoUnit.MONTHS.between(m1, m2);
    }
}
