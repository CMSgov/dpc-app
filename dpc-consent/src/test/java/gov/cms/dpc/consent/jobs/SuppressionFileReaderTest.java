package gov.cms.dpc.consent.jobs;

import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.exceptions.InvalidSuppressionRecordException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

public class SuppressionFileReaderTest {

    @Test
    public void testEntityFromLine() throws IOException {
        BlueButtonClient bfdClient = mockBfdClient();
        SuppressionFileReader reader = new SuppressionFileReader(Path.of("./src/test/resources/synthetic-1800-files/valid/T#EFT.ON.ACO.NGD1800.DPRF.D191029.T1135430"), bfdClient);

        Optional<ConsentEntity> result = reader.entityFromLine();

        assertFalse(result.isPresent()); // line 1 = header
        result = reader.entityFromLine();
        assertFalse(result.isPresent()); // line 2 = SA pref only
        result = reader.entityFromLine();
        assertFalse(result.isPresent()); // line 3 = SA pref only
        result = reader.entityFromLine();
        assertTrue(result.isPresent()); // line 4 = sharing Y

        ConsentEntity consent = result.get();
        assertEquals("1000009420", consent.getHicn());
        assertEquals("3456789", consent.getMbi());
        assertEquals("OPTIN", consent.getPolicyCode());
        assertEquals(LocalDate.parse("2019-10-29"), consent.getEffectiveDate());
    }

    @Test
    public void testSetDates() throws IOException {
        SuppressionFileReader reader = new SuppressionFileReader(Path.of(""), null);
        ConsentEntity consent = new ConsentEntity();
        reader.line = "1000087481 1847800005John                          Mitchell                      Doe                                     198203218702 E Fake St.                                        Apt. 63L                                               Region                                                 Las Vegas                               NV423139954M20190618201907011-800TY201907011-800TNT9992WeCare Medical                                                        ";
        reader.consent = consent;

        reader.setDates();

        assertEquals(LocalDate.parse("2019-07-01"), consent.getEffectiveDate());
    }

    @Test
    public void testSetIdentifiers() throws IOException {
        BlueButtonClient bfdClient = mockBfdClient();
        SuppressionFileReader reader = new SuppressionFileReader(Path.of(""), bfdClient);
        ConsentEntity consent = new ConsentEntity();
        reader.line = "1000087481 1847800005John                          Mitchell                      Doe                                     198203218702 E Fake St.                                        Apt. 63L                                               Region                                                 Las Vegas                               NV423139954M20190618201907011-800TY201907011-800TNT9992WeCare Medical                                                        ";
        reader.consent = consent;
        reader.setIdentifiers();

        assertEquals("1000087481", consent.getHicn());
        assertEquals("3456789", consent.getMbi());
        assertEquals("20140000008325", consent.getBfdPatientId());
    }

    @Test
    public void testSetCodes() throws IOException {
        SuppressionFileReader reader = new SuppressionFileReader(Path.of(""), null);

        ConsentEntity consent = new ConsentEntity();
        reader.line = "1000087481 1847800005John                          Mitchell                      Doe                                     198203218702 E Fake St.                                        Apt. 63L                                               Region                                                 Las Vegas                               NV423139954M20190618201907011-800TY201907011-800TNT9992WeCare Medical                                                        ";
        reader.consent = consent;

        reader.setCodes();

        assertEquals("OPTIN", consent.getPolicyCode());
        assertEquals("TREAT", consent.getPurposeCode());
        assertEquals("64292-6", consent.getLoincCode());
        assertEquals("patient-privacy", consent.getScopeCode());
    }

    public static BlueButtonClient mockBfdClient() {
        Patient p = new Patient();

        Identifier beneId = new Identifier();
        beneId.setSystem("https://bluebutton.cms.gov/resources/variables/bene_id");
        beneId.setValue("20140000008325");
        p.addIdentifier(beneId);

        Identifier mbi = new Identifier();
        mbi.setSystem("http://hl7.org/fhir/sid/us-mbi");
        mbi.setValue("3456789");
        p.addIdentifier(mbi);

        Bundle b = new Bundle();
        Bundle.BundleEntryComponent bec = new Bundle.BundleEntryComponent();
        bec.setResource(p);
        b.addEntry(bec);

        BlueButtonClient client = Mockito.mock(BlueButtonClient.class);
        Mockito.when(client.searchPatientFromServerByHICN(Mockito.anyString())).thenReturn(b);
        return client;
    }
}
