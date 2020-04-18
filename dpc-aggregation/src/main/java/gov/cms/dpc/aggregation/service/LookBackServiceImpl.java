package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.aggregation.dao.RosterDAO;
import gov.cms.dpc.aggregation.engine.OperationsConfig;
import io.dropwizard.hibernate.UnitOfWork;
import io.vavr.control.Try;
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
    public UUID getProviderIDFromRoster(UUID orgUUID, String ambiguousID, String patientMBI) {
        //Expect only one roster for the parameters, otherwise return null
        return Try.of(() -> rosterDAO.retrieveProviderIDFromRoster(orgUUID, UUID.fromString(ambiguousID), patientMBI)).getOrElse((UUID) null);
    }

    @Override
    public boolean hasClaimWithin(ExplanationOfBenefit explanationOfBenefit, UUID organizationUUID, UUID providerUUID, long withinMonth) {
        Optional<Date> billingPeriod = Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getBillablePeriod)
                .map(Period::getEnd);

        Optional<String> providerID = Optional.ofNullable(providerUUID)
                .map(UUID::toString);

        Optional<String> organizationID = Optional.ofNullable(organizationUUID)
                .map(UUID::toString);

        Optional<String> eobProviderID = Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getProvider)
                .map(Element::getId);

        Optional<String> eobOrganizationID = Optional.ofNullable(explanationOfBenefit)
                .map(ExplanationOfBenefit::getOrganization)
                .map(Element::getId);

        return billingPeriod.isPresent()
                && providerID.isPresent() && eobProviderID.isPresent()
                && organizationID.isPresent() && eobOrganizationID.isPresent()
                && getMonthsDifference(billingPeriod.get(), operationsConfig.getLookBackDate()) < withinMonth
                && providerID.get().equals(eobProviderID.get())
                && organizationID.get().equals(eobOrganizationID.get());
    }

    private long getMonthsDifference(Date date1, Date date2) {
        YearMonth m1 = YearMonth.from(date1.toInstant().atZone(ZoneOffset.UTC));
        YearMonth m2 = YearMonth.from(date2.toInstant().atZone(ZoneOffset.UTC));
        return ChronoUnit.MONTHS.between(m1, m2);
    }
}
