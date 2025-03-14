package gov.cms.dpc.common.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SeedProcessorUnitTest {

    private static final String CSV = "test_associations-dpr.csv";
    private static final String providerNPI = NPIUtil.generateNPI();
    private static final UUID orgID = UUID.randomUUID();

    private static Map<String, List<Pair<String, String>>> groupedPairs;

    @BeforeAll
    static void setUp() throws IOException {
        InputStream resource = SeedProcessorUnitTest.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Cannot find seeds file", SeedProcessorUnitTest.class.getName(), CSV);
        }
        groupedPairs = SeedProcessor.extractProviderMap(resource);
    }

    @Test
    void testExtractProviderMap() {
        groupedPairs.forEach((providerId, pairList) ->
                pairList.forEach(pair ->
                        assertEquals(providerId, pair.getLeft())
                )
        );
    }

    @Test
    void testCreateBaseAttributionGroup() {
        Group rosterGroup = SeedProcessor.createBaseAttributionGroup(providerNPI, orgID.toString());
        assertAll(
                () -> assertEquals(Group.GroupType.PERSON, rosterGroup.getType()),
                () -> assertTrue(rosterGroup.getActive()),
                () -> assertEquals(orgID.toString(), rosterGroup.getMeta().getTag().get(0).getCode()),
                () -> assertFalse(rosterGroup.getCharacteristicFirstRep().getExclude())
        );
    }

    @Test
    void testGenerateAttributionGroup() {
        Map<String, Reference> patientReferences = Map.of();
        groupedPairs.entrySet().forEach(roster -> {
            Group attributionGroup = SeedProcessor.generateAttributionGroup(roster, orgID, patientReferences);
            attributionGroup.getMember().forEach(member -> assertFalse(member.getInactive()));
        });
    }

    @Test
    void testGenerateAttributionBundle() {
        List<Bundle> bundles = groupedPairs.entrySet().stream().map((Map.Entry<String, List<Pair<String, String>>> entry) ->
                SeedProcessor.generateAttributionBundle(entry, orgID)).toList();
        bundles.forEach(bundle -> {
            assertEquals("Roster/12345", bundle.getId());
            assertEquals(Bundle.BundleType.COLLECTION, bundle.getType());

            Resource firstEntry = bundle.getEntryFirstRep().getResource();
            assertEquals(ResourceType.Practitioner, firstEntry.getResourceType());
            assertEquals(orgID.toString(), firstEntry.getMeta().getTag().get(0).getCode());

            bundle.getEntry().stream().skip(1).forEach(entry ->
                    assertEquals(ResourceType.Patient, entry.getResource().getResourceType())
            );
        });
    }
}
