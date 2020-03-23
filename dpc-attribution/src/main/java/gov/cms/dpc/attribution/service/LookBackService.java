package gov.cms.dpc.attribution.service;

import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

public class LookBackService {

    private DataService dataService;

    @Inject
    public LookBackService(DataService dataService) {
        this.dataService = dataService;
    }

    public boolean isValidProviderPatientRelation(UUID organizationID, UUID patientID, UUID providerID, long withinMonth) {
        try {
            Bundle bundle = dataService.retrieveData(organizationID, providerID, Collections.singletonList(patientID.toString()), ResourceType.ExplanationOfBenefit);
            return hasClaimWithin(bundle, providerID, organizationID, withinMonth);
        } catch (Exception e) {
            throw new WebApplicationException("Failed to look back", HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }

    private boolean hasClaimWithin(Bundle bundle, UUID providerID, UUID organizationID, long withinMonth) {
        return bundle.getEntry().stream()
                .map(e -> (ExplanationOfBenefit) e.getResource())
                .anyMatch(c ->
                        getMonthsDifference(c.getBillablePeriod().getEnd(), new Date()) < withinMonth
                                && c.getProvider().getId().equals(providerID.toString())
                                && c.getOrganization().getId().equals(organizationID.toString())
                );
    }

    private long getMonthsDifference(Date date1, Date date2) {
        YearMonth m1 = YearMonth.from(date1.toInstant().atZone(ZoneOffset.UTC));
        YearMonth m2 = YearMonth.from(date2.toInstant().atZone(ZoneOffset.UTC));
        return ChronoUnit.MONTHS.between(m1, m2);
    }
}
