package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.aggregation.dao.OrganizationDAO;
import gov.cms.dpc.aggregation.dao.RosterDAO;
import gov.cms.dpc.aggregation.engine.OperationsConfig;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import io.dropwizard.hibernate.UnitOfWork;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;

import javax.inject.Inject;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class LookBackServiceImpl implements LookBackService {

    private final RosterDAO rosterDAO;
    private final OrganizationDAO organizationDAO;
    private final OperationsConfig operationsConfig;

    @Inject
    public LookBackServiceImpl(RosterDAO rosterDAO, OrganizationDAO organizationDAO, OperationsConfig operationsConfig) {
        this.rosterDAO = rosterDAO;
        this.organizationDAO = organizationDAO;
        this.operationsConfig = operationsConfig;
    }

    @Override
    @UnitOfWork(readOnly = true)
    public String getProviderNPIFromRoster(UUID orgUUID, String providerOrRosterID, String patientMBI) {
        //Expect only one roster for the parameters, otherwise return null
        return Try.of(() -> rosterDAO.retrieveProviderNPIFromRoster(orgUUID, UUID.fromString(providerOrRosterID), patientMBI)).getOrNull();
    }

    @Override
    @UnitOfWork(readOnly = true)
    public boolean hasClaimWithin(ExplanationOfBenefit explanationOfBenefit, UUID organizationUUID, String providerUUID, long withinMonth) {
        Optional<Date> billingPeriod = Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getBillablePeriod)
                .map(Period::getEnd);

        Optional<String> providerID = Optional.ofNullable(providerUUID);

        Optional<String> organizationID = organizationDAO.fetchOrganizationNPI(organizationUUID);

        Optional<String> eobOrganizationID = Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getOrganization)
                .map(Reference::getIdentifier)
                .filter(i -> DPCIdentifierSystem.NPPES.getSystem().equals(i.getSystem()))
                .map(Identifier::getValue);

        Set<String> eobProviderNPIs = extractPractionerNPIs(explanationOfBenefit);

        return billingPeriod.isPresent()
                && providerID.isPresent()
                && organizationID.isPresent() && eobOrganizationID.isPresent()
                && getMonthsDifference(billingPeriod.get(), operationsConfig.getLookBackDate()) < withinMonth
                && eobProviderNPIs.contains(providerID.get())
                && organizationID.get().equals(eobOrganizationID.get());
    }

    private Set<String> extractPractionerNPIs(ExplanationOfBenefit explanationOfBenefit) {
        Set<String> eobProviderNPIs = new HashSet<>();
        Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getProvider)
                .map(Reference::getIdentifier)
                .filter(i -> DPCIdentifierSystem.NPPES.getSystem().equals(i.getSystem()))
                .map(Identifier::getValue)
                .filter(StringUtils::isNotBlank)
                .ifPresent(eobProviderNPIs::add);

        Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getCareTeam)
                .ifPresent(careTeamComponents -> {
                    careTeamComponents.stream()
                            .filter(ExplanationOfBenefit.CareTeamComponent::hasProvider)
                            .map(ExplanationOfBenefit.CareTeamComponent::getProvider)
                            .map(Reference::getIdentifier)
                            .filter(i -> DPCIdentifierSystem.NPPES.getSystem().equals(i.getSystem()))
                            .map(Identifier::getValue)
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
