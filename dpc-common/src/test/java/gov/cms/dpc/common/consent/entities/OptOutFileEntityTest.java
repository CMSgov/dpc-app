package gov.cms.dpc.common.consent.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptOutFileEntityTest {

	@Test
    void testGettersAndSetters() {
        OptOutFileEntity file = new OptOutFileEntity();
        UUID id = UUID.randomUUID();
        String name = "opt out file name";
        LocalDate timestamp = LocalDate.now();
        String status = "import status";
        OffsetDateTime createdAt = OffsetDateTime.now();
        OffsetDateTime updatedAt = OffsetDateTime.now();

        file.setId(id);
        file.setName(name);
        file.setTimestamp(timestamp);
        file.setImportStatus(status);
        file.setCreatedAt(createdAt);
        file.setUpdatedAt(updatedAt);

        assertEquals(id, file.getId());
        assertEquals(name, file.getName());
        assertEquals(timestamp, file.getTimestamp());
        assertEquals(status, file.getImportStatus());
        assertEquals(createdAt, file.getCreatedAt());
        assertEquals(updatedAt, file.getUpdatedAt());
	}
}
