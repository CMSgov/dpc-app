package gov.cms.dpc.attribution.service;

import gov.cms.dpc.queue.service.DataService;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.WebApplicationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@ExtendWith(BufferedLoggerHandler.class)
public class LookBackServiceTest {

    private UUID providerID = UUID.randomUUID();
    private UUID orgID = UUID.randomUUID();

    private LookBackService lookBackService;
    private ExplanationOfBenefit eob;

    @Mock
    private DataService dataService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        lookBackService = new LookBackServiceImpl(dataService);
        eob = new ExplanationOfBenefit();
        eob.setBillablePeriod(new Period());
        eob.setProvider(new Reference());
        eob.getProvider().setId(providerID.toString());
        eob.setOrganization(new Reference());
        eob.getOrganization().setId(orgID.toString());
        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setResource(eob);
        Bundle bundle = new Bundle();
        bundle.setEntry(Collections.singletonList(entry));
        Mockito.doReturn(bundle).when(dataService).retrieveData(Mockito.any(), Mockito.any(), Mockito.anyList(), Mockito.any());
    }

    @Test
    public void testClaimWithin() {
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        boolean result = lookBackService.isValidProviderPatientRelation(orgID, UUID.randomUUID(), providerID, 1);
        Assertions.assertTrue(result);

        dateTime = OffsetDateTime.now(ZoneOffset.UTC).minusMonths(1);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        result = lookBackService.isValidProviderPatientRelation(orgID, UUID.randomUUID(), providerID, 1);
        Assertions.assertFalse(result);

        dateTime = OffsetDateTime.now(ZoneOffset.UTC).plusMonths(1);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        result = lookBackService.isValidProviderPatientRelation(orgID, UUID.randomUUID(), providerID, 1);
        Assertions.assertTrue(result);
    }

    @Test
    public void testNonMatchingOrgID() {
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        boolean result = lookBackService.isValidProviderPatientRelation(UUID.randomUUID(), UUID.randomUUID(), providerID, 1);
        Assertions.assertFalse(result);
    }

    @Test
    public void testNonMatchingProviderID() {
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneOffset.UTC);
        eob.getBillablePeriod().setEnd(Date.from(dateTime.toInstant()));

        boolean result = lookBackService.isValidProviderPatientRelation(orgID, UUID.randomUUID(), UUID.randomUUID(), 1);
        Assertions.assertFalse(result);
    }

    @Test
    public void testDataServiceReturnsNotBundleType() {
        Mockito.doReturn(new OperationOutcome()).when(dataService).retrieveData(Mockito.any(), Mockito.any(), Mockito.anyList(), Mockito.any());

        Assertions.assertThrows(WebApplicationException.class, () -> {
            lookBackService.isValidProviderPatientRelation(orgID, UUID.randomUUID(), UUID.randomUUID(), 1);
        });
    }

    @Test
    public void testDataServiceThrowsException() {
        Mockito.doThrow(new RuntimeException("error")).when(dataService).retrieveData(Mockito.any(), Mockito.any(), Mockito.anyList(), Mockito.any());

        Assertions.assertThrows(WebApplicationException.class, () -> {
            lookBackService.isValidProviderPatientRelation(orgID, UUID.randomUUID(), UUID.randomUUID(), 1);
        });
    }
}
