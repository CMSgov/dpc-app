package gov.cms.dpc.consent.jobs;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.consent.exceptions.InvalidSuppressionRecordException;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SuppressionFileUtilsTest {

    @Test
    public void testIs1800File() {
       assertTrue(SuppressionFileUtils.is1800File(Paths.get("./src/test/resources/T#EFT.ON.ACO.NGD1800.DPRF.D181120.T1000009")));
    }

    @Test
    public void testEntityFromLine() {
        Optional<ConsentEntity> result = SuppressionFileUtils.entityFromLine("1000087481 1847800005John                          Mitchell                      Doe                                     198203218702 E Fake St.                                        Apt. 63L                                               Region                                                 Las Vegas                               NV423139954M20190618201907011-800TY201907011-800TNT9992WeCare Medical                                                        ", "", 0);
        assertTrue(result.isPresent());
        ConsentEntity consent = result.get();
        assertEquals("1000087481", consent.getHicn());
        assertEquals("OPTIN", consent.getPolicyCode());
        assertEquals(LocalDate.parse("2019-07-01"), consent.getEffectiveDate());
    }

    @Test
    public void testEntityFromLine_InvalidSource() {
        assertThrows(InvalidSuppressionRecordException.class, () -> {
            SuppressionFileUtils.entityFromLine("1000050218 1120500001Janice                        Marie                         J                                       19700227288 Waterpool Dr.                                      AddressLine2                                           City                                                   FakeCity                                NY110390889U2019030120190719aaaaaTN               T9992                                                                      ", "", 0);
        });
    }

    @Test
    public void testEntityFromLine_Header() {
        Optional<ConsentEntity> result = SuppressionFileUtils.entityFromLine("HDR_BENEDATASHR20191011", "", 0);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEntityFromLine_Trailer() {
        Optional<ConsentEntity> result = SuppressionFileUtils.entityFromLine("TRL_BENEDATASHR20191011        10", "", 0);
        assertTrue(result.isEmpty());
    }
}
