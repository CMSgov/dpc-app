package gov.cms.dpc.fhir.helpers;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
@DisplayName("DPC collectors")


class DPCCollectorsUnitTest {
    @Test
@DisplayName("Collect objects from singleton list 🥳")

    void testSingleOrNoneHappyPath() {
        List<Integer> list = List.of(1);
        assertEquals(Optional.of(1), list.stream().collect(DPCCollectors.singleOrNone()));
    }

    @Test
@DisplayName("Collect objects from empty list 🥳")

    void testSingleOrNoneEmptyList() {
        List<Integer> list = List.of();
        assertEquals(Optional.empty(), list.stream().collect(DPCCollectors.singleOrNone()));
    }

    @Test
@DisplayName("Collect objects from full list 🥳")

    void testSingleOrNoneFullList() {
        List<Integer> list = List.of(1, 2, 3);
        assertEquals(Optional.empty(), list.stream().collect(DPCCollectors.singleOrNone()));
    }
}