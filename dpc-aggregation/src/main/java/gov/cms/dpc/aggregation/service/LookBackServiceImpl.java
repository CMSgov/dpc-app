package gov.cms.dpc.aggregation.service;

import com.google.common.base.Joiner;
import gov.cms.dpc.aggregation.dao.OrganizationDAO;
import gov.cms.dpc.aggregation.dao.RosterDAO;
import gov.cms.dpc.aggregation.engine.OperationsConfig;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.inject.Inject;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static gov.cms.dpc.common.MDCConstants.EOB_ID;

public class LookBackServiceImpl implements LookBackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookBackService.class);

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
        return rosterDAO.retrieveProviderNPIFromRoster(orgUUID, UUID.fromString(providerOrRosterID), patientMBI).orElse(null);
    }

    @Override
    @UnitOfWork(readOnly = true)
    public boolean hasClaimWithin(ExplanationOfBenefit explanationOfBenefit, UUID organizationUUID, String providerNPI, long withinMonth) {
        MDC.put(EOB_ID, explanationOfBenefit.getId());
        Date billingPeriod = Optional.of(explanationOfBenefit)
                .map(ExplanationOfBenefit::getBillablePeriod)
                .map(Period::getEnd)
                .orElse(null);

        String organizationID = organizationDAO.fetchOrganizationNPI(organizationUUID).orElse(null);

        String eobOrganizationID = Optional.of(explanationOfBenefit)
                .map(ExplanationOfBenefit::getOrganization)
                .map(Reference::getIdentifier)
                .filter(i -> DPCIdentifierSystem.NPPES.getSystem().equals(i.getSystem()))
                .map(Identifier::getValue)
                .orElse(null);

        Pair<String, Set<String>> npis = extractPractionerNPIs(explanationOfBenefit);
        Set<String> allNPIs = new HashSet<>(npis.getRight());
        allNPIs.add(npis.getLeft());

        LookBackAnswer lookBackAnswer = passLookBack(billingPeriod, providerNPI, organizationID, eobOrganizationID, allNPIs, withinMonth);
        LOGGER.info("billingPeriodDate={}, lookBackDate={}, monthsDifference={}, eobProvider={}, eobCareTeamProviders={}, jobProvider={}, eobOrganization={}, jobOrganization={}, withinLimit={}, eobProviderMatch={}, eobOrganizationMatch={}",
                billingPeriod, operationsConfig.getLookBackDate(), lookBackAnswer.getBillingDateMonthsFromNow(), npis.getLeft(), Joiner.on(";").join(npis.getRight()), providerNPI, eobOrganizationID,
                organizationID, lookBackAnswer.isMatchLookBackLimitCriteria(), lookBackAnswer.isMatchProvidersCriteria(), lookBackAnswer.isMatchOrganizationCriteria());

        MDC.remove(EOB_ID);
        return lookBackAnswer.answer();
    }

    private LookBackAnswer passLookBack(Date billingPeriod, String providerID, String organizationID, String eobOrganizationID, Set<String> eobProviderNPIs, long withinMonth) {
        Optional<Date> optionalBillingPeriod = Optional.ofNullable(billingPeriod);
        Optional<String> optionalProviderID = Optional.ofNullable(providerID);
        Optional<String> optionalOrganizationID = Optional.ofNullable(organizationID);
        Optional<String> optionalEobOrganizationID = Optional.ofNullable(eobOrganizationID);

        LookBackAnswer result = new LookBackAnswer();
        if (optionalBillingPeriod.isPresent() && optionalProviderID.isPresent() && optionalOrganizationID.isPresent() && optionalEobOrganizationID.isPresent()) {
            long lookBackMonthsDifference = getMonthsDifference(optionalBillingPeriod.get(), operationsConfig.getLookBackDate());
            boolean eobContainsProvider = eobProviderNPIs.contains(optionalProviderID.get());
            boolean eobRelatedToOrganization = optionalOrganizationID.get().equals(optionalEobOrganizationID.get());
            boolean eobWithinLookBackLimit = lookBackMonthsDifference < withinMonth;
            result.setMatchLookBackLimitCriteria(eobWithinLookBackLimit);
            result.setMatchOrganizationCriteria(eobRelatedToOrganization);
            result.setMatchProvidersCriteria(eobContainsProvider);
            result.setBillingDateMonthsFromNow(lookBackMonthsDifference);
        }
        return result;
    }

    private Pair<String, Set<String>> extractPractionerNPIs(ExplanationOfBenefit explanationOfBenefit) {
        String providerNPI = Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getProvider)
                .map(Reference::getIdentifier)
                .filter(i -> DPCIdentifierSystem.NPPES.getSystem().equals(i.getSystem()))
                .map(Identifier::getValue)
                .filter(StringUtils::isNotBlank)
                .orElse(null);

        Set<String> careTeamProviders = new HashSet<>();
        Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getCareTeam)
                .ifPresent(careTeamComponents -> careTeamComponents.stream()
                        .filter(ExplanationOfBenefit.CareTeamComponent::hasProvider)
                        .map(ExplanationOfBenefit.CareTeamComponent::getProvider)
                        .map(Reference::getIdentifier)
                        .filter(i -> DPCIdentifierSystem.NPPES.getSystem().equals(i.getSystem()))
                        .map(Identifier::getValue)
                        .filter(StringUtils::isNotBlank)
                        .forEach(careTeamProviders::add));

        return Pair.of(providerNPI, careTeamProviders);
    }

    private long getMonthsDifference(Date date1, Date date2) {
        YearMonth m1 = YearMonth.from(date1.toInstant().atZone(ZoneOffset.UTC));
        YearMonth m2 = YearMonth.from(date2.toInstant().atZone(ZoneOffset.UTC));
        return ChronoUnit.MONTHS.between(m1, m2);
    }
}
