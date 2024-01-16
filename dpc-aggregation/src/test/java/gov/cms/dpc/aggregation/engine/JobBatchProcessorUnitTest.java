package gov.cms.dpc.aggregation.engine;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.aggregation.service.ConsentService;
import gov.cms.dpc.aggregation.service.LookBackService;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import io.reactivex.Flowable;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class JobBatchProcessorUnitTest {
    @Mock private BlueButtonClient blueButtonClient;
    @Mock private FhirContext fhirContext;
    @Mock private OperationsConfig operationsConfig;
    @Mock private LookBackService lookBackService;
    @Mock private ConsentService consentService;

    private MetricRegistry metricRegistry = new MetricRegistry();

    private JobBatchProcessor jobBatchProcessor = new JobBatchProcessor(
            blueButtonClient, fhirContext, metricRegistry, operationsConfig, lookBackService, consentService);

    /*
        As a rule, we shouldn't be writing tests for private methods, but this method isn't in use yet so this seemed
        like the best way to verify it works.
        TODO: Remove these tests after getMBIs is put into use and we have unit tests for the JobBatchProcessor class (DPC-3562).
     */

    @Test
    public void testGetMBIs_oneResource() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.MBI.getSystem());
        identifier.setValue("4S41C00AA00");

        Patient patient = new Patient();
        patient.setIdentifier(List.of(identifier));

        Method getMBIs = JobBatchProcessor.class.getDeclaredMethod("getMBIs", Flowable.class);
        getMBIs.setAccessible(true);

        List<String> mbis = (List<String>) getMBIs.invoke(jobBatchProcessor, Flowable.just(patient));

        assertEquals(1, mbis.size());
        assertEquals(identifier.getValue(), mbis.get(0));
    }

    @Test
    public void testGetMBIs_manyResources() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Identifier mbi1 = new Identifier();
        mbi1.setSystem(DPCIdentifierSystem.MBI.getSystem());
        mbi1.setValue("4S41C00AA00");

        Identifier mbi2 = new Identifier();
        mbi2.setSystem(DPCIdentifierSystem.MBI.getSystem());
        mbi2.setValue("4S41C00AA01");

        Identifier badMbi = new Identifier();
        badMbi.setSystem(DPCIdentifierSystem.MBI.getSystem());
        badMbi.setValue("bad_mbi");

        Identifier beneId = new Identifier();
        beneId.setSystem(DPCIdentifierSystem.BENE_ID.getSystem());
        beneId.setValue("bene_id");

        Patient patient = new Patient();
        patient.setIdentifier(List.of(mbi1, mbi2, beneId, badMbi));

        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        Coverage coverage = new Coverage();

        Method getMBIs = JobBatchProcessor.class.getDeclaredMethod("getMBIs", Flowable.class);
        getMBIs.setAccessible(true);

        List<String> mbis = (List<String>) getMBIs.invoke(jobBatchProcessor, Flowable.just(patient, eob, coverage));

        assertEquals(2, mbis.size());
        assertTrue(mbis.contains(mbi1.getValue()));
        assertTrue(mbis.contains(mbi2.getValue()));
    }
}