package gov.cms.dpc.common.consent.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsentEntityTest {

	@Test
    void testGettersAndSetters() {
        ConsentEntity consent = new ConsentEntity();
        UUID id = UUID.randomUUID();
        String mbi = "mbi";
        String hicn = "hicn";
        String bfdPatientId = "bfd patient id";
        LocalDate effectiveDate = LocalDate.now();
        String policyCode = "policy";
        String purposeCode = "purpose";
        String loincCode = "loinc";
        String scopeCode = "scope";
        OffsetDateTime createdAt = OffsetDateTime.now();
        OffsetDateTime updatedAt = OffsetDateTime.now();
        String sourceCode = "source";
        OptOutFileEntity file = new OptOutFileEntity();

        consent.setId(id);
        consent.setMbi(mbi);
        consent.setHicn(hicn);
        consent.setBfdPatientId(bfdPatientId);
        consent.setEffectiveDate(effectiveDate);
        consent.setPolicyCode(policyCode);
        consent.setPurposeCode(purposeCode);
        consent.setLoincCode(loincCode);
        consent.setScopeCode(scopeCode);
        consent.setCreatedAt(createdAt);
        consent.setUpdatedAt(updatedAt);
        consent.setSourceCode(sourceCode);
        consent.setOptOutFile(file);

        assertEquals(id, consent.getId());
        assertEquals(mbi, consent.getMbi());
        assertEquals(hicn, consent.getHicn());
        assertEquals(bfdPatientId, consent.getBfdPatientId());
        assertEquals(effectiveDate, consent.getEffectiveDate());
        assertEquals(policyCode, consent.getPolicyCode());
        assertEquals(purposeCode, consent.getPurposeCode());
        assertEquals(loincCode, consent.getLoincCode());
        assertEquals(scopeCode, consent.getScopeCode());
        assertEquals(createdAt, consent.getCreatedAt());
        assertEquals(updatedAt, consent.getUpdatedAt());
        assertEquals(sourceCode, consent.getSourceCode());
        assertEquals(file, consent.getOptOutFile());
	}
}
