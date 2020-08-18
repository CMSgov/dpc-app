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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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

        if (billingPeriod.isEmpty()) {
            LOGGER.info("billingPeriod=empty");
        } else {
            LOGGER.info("billingPeriod={}", billingPeriod.get());
        }

        if (eobOrganizationID.isEmpty()) {
            LOGGER.info("eobOrganizationID=empty");
        } else {
            LOGGER.info("eobOrganizationID={}", eobOrganizationID.get());
        }

        if (billingPeriod.isEmpty() || providerID.isEmpty() || organizationID.isEmpty() || eobOrganizationID.isEmpty()) {
            LOGGER.info("eob BillingPeriod or job providerID or job organizationID or eob OrganizationID are null");
            return false;
        }

        long lookBackMonthsDifference = getMonthsDifference(billingPeriod.get(), operationsConfig.getLookBackDate());
        boolean eobContainsProvider = eobProviderNPIs.contains(providerID.get());
        boolean eobRelatedToOrganization = organizationID.get().equals(eobOrganizationID.get());
        boolean eobWithinLookBackLimit = lookBackMonthsDifference < withinMonth;

        boolean hasClaim = eobWithinLookBackLimit
                && eobContainsProvider
                && eobRelatedToOrganization;

        LOGGER.info("LookBack stats eobWithinLookBackLimit={}, eobContainsProvider={}, eobRelatedToOrganization={}, eobMonthsDifference={}, hasClaim={}",
                eobWithinLookBackLimit, eobContainsProvider, eobRelatedToOrganization, lookBackMonthsDifference, hasClaim);

        return hasClaim;
    }

    private Set<String> extractPractionerNPIs(ExplanationOfBenefit explanationOfBenefit) {
        Set<String> eobProviderNPIs = new HashSet<>();
        Optional<String> providerNPI = Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getProvider)
                .map(Reference::getIdentifier)
                .filter(i -> DPCIdentifierSystem.NPPES.getSystem().equals(i.getSystem()))
                .map(Identifier::getValue)
                .filter(StringUtils::isNotBlank);

        if (providerNPI.isEmpty()) {
            LOGGER.info("providerNPI=empty");
        } else {
            LOGGER.info("providerNPI={}", providerNPI.get());
            eobProviderNPIs.add(providerNPI.get());
        }

        Optional<List<ExplanationOfBenefit.CareTeamComponent>> careTeam = Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getCareTeam);

        if (careTeam.isEmpty()) {
            LOGGER.info("careTeam=empty");
        }

        careTeam
                .ifPresent(careTeamComponents -> {
                    List<String> npisInCareTeam = careTeamComponents.stream()
                            .filter(ExplanationOfBenefit.CareTeamComponent::hasProvider)
                            .map(ExplanationOfBenefit.CareTeamComponent::getProvider)
                            .map(Reference::getIdentifier)
                            .filter(i -> DPCIdentifierSystem.NPPES.getSystem().equals(i.getSystem()))
                            .map(Identifier::getValue)
                            .filter(StringUtils::isNotBlank)
                            .collect(Collectors.toList());

                    LOGGER.info("careTeamNPIs={}", npisInCareTeam);
                    eobProviderNPIs.addAll(npisInCareTeam);
                });

        return eobProviderNPIs;
    }

    private long getMonthsDifference(Date date1, Date date2) {
        YearMonth m1 = YearMonth.from(date1.toInstant().atZone(ZoneOffset.UTC));
        YearMonth m2 = YearMonth.from(date2.toInstant().atZone(ZoneOffset.UTC));
        return ChronoUnit.MONTHS.between(m1, m2);
    }
}
