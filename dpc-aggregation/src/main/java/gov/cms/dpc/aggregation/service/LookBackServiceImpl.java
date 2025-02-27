package gov.cms.dpc.aggregation.service;

import com.google.common.base.Joiner;
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
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static gov.cms.dpc.common.MDCConstants.EOB_ID;

public class LookBackServiceImpl implements LookBackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookBackService.class);

    private final OperationsConfig operationsConfig;

    @Inject
    public LookBackServiceImpl(OperationsConfig operationsConfig) {
        this.operationsConfig = operationsConfig;
    }

    @Override
    @UnitOfWork(readOnly = true)
    @SuppressWarnings("JdkObsolete") // Date class used by FHIR stu3 Period model
    public LookBackAnswer getLookBackAnswer(ExplanationOfBenefit explanationOfBenefit, String organizationNPI, String practitionerNPI, long withinMonth) {
        MDC.put(EOB_ID, explanationOfBenefit.getId());
        YearMonth billingPeriod = Optional.of(explanationOfBenefit)
                .map(ExplanationOfBenefit::getBillablePeriod)
                .map(Period::getEnd)
                .map(date -> YearMonth.from(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()))
                .orElse(null);

        String eobOrganizationNPI = Optional.of(explanationOfBenefit)
                .map(ExplanationOfBenefit::getOrganization)
                .map(Reference::getIdentifier)
                .filter(i -> DPCIdentifierSystem.NPPES.getSystem().equals(i.getSystem()))
                .map(Identifier::getValue)
                .orElse(null);

        Pair<String, Set<String>> npis = extractProviderNPIs(explanationOfBenefit);
        Set<String> allNPIs = new HashSet<>(npis.getRight());
        allNPIs.add(npis.getLeft());

        LookBackAnswer lookBackAnswer = new LookBackAnswer(practitionerNPI, organizationNPI, withinMonth, operationsConfig.getLookBackDate())
                .addEobBillingPeriod(billingPeriod)
                .addEobOrganization(eobOrganizationNPI)
                .addEobProviders(allNPIs);
        LOGGER.trace("billingPeriodDate={}, lookBackDate={}, monthsDifference={}, eobProvider={}, eobCareTeamProviders={}, jobProvider={}, eobOrganization={}, jobOrganization={}, withinLimit={}, eobProviderMatch={}, eobOrganizationMatch={}",
                billingPeriod, operationsConfig.getLookBackDate(), lookBackAnswer.calculatedMonthDifference(), npis.getLeft(), Joiner.on(";").join(npis.getRight()), practitionerNPI, eobOrganizationNPI,
                organizationNPI, lookBackAnswer.matchDateCriteria(), lookBackAnswer.practitionerMatchEob(), lookBackAnswer.orgMatchEob());

        MDC.remove(EOB_ID);
        return lookBackAnswer;
    }

    private Pair<String, Set<String>> extractProviderNPIs(ExplanationOfBenefit explanationOfBenefit) {
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
}
