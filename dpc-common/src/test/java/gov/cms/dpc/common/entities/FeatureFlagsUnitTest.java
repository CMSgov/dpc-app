package gov.cms.dpc.common.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FeatureFlagsUnitTest {

    @Test
    void testSetGetBooleanFeature() {
        FeatureFlags flags = new FeatureFlags();
        assertFalse(flags.getBooleanFeature("missing-flag").isPresent(), "Flag should not have been present");
        flags.setFeature("a_feature_flag", true);
        assertTrue(flags.getBooleanFeature("a_feature_flag").isPresent(), "Flag should  have been present");
        assertTrue(flags.getBooleanFeature("a_feature_flag").get(), "Flag should have been true");
    }

    @Test
    void testSetGetIntegerFeature() {
        FeatureFlags flags = new FeatureFlags();
        assertFalse(flags.getIntegerFeature("missing-flag").isPresent(), "Flag should not have been present");
        flags.setFeature("a_feature_flag", 10);
        assertTrue(flags.getIntegerFeature("a_feature_flag").isPresent(), "Flag should have been present");
        assertEquals(10, flags.getIntegerFeature("a_feature_flag").get());
    }

    @Test
    void testSetGetDoubleFeature() {
        FeatureFlags flags = new FeatureFlags();
        assertFalse(flags.getDoubleFeature("missing-flag").isPresent(), "Flag should not have been present");
        flags.setFeature("a_feature_flag", 10.5);
        assertTrue(flags.getDoubleFeature("a_feature_flag").isPresent(), "Flag should have been present");
        assertEquals(10.5, flags.getDoubleFeature("a_feature_flag").get());
    }

    @Test
    void testSetGetLongFeature() {
        FeatureFlags flags = new FeatureFlags();
        assertFalse(flags.getLongFeature("missing-flag").isPresent(), "Flag should not have been present");
        flags.setFeature("a_feature_flag", 10L);
        assertTrue(flags.getLongFeature("a_feature_flag").isPresent(), "Flag should have been present");
        assertEquals(10L, flags.getLongFeature("a_feature_flag").get());
    }

    @Test
    void testSetGetStringFeature() {
        FeatureFlags flags = new FeatureFlags();
        assertFalse(flags.getStringFeature("missing-flag").isPresent(), "Flag should not have been present");
        flags.setFeature("a_feature_flag", "some value");
        assertTrue(flags.getStringFeature("a_feature_flag").isPresent(), "Flag should have been present");
        assertEquals("some value", flags.getStringFeature("a_feature_flag").get());
    }

    @Test
    void testFeatureRemoval() {
        FeatureFlags flags = new FeatureFlags();
        flags.setFeature("a_feature_flag", "some value");
        assertTrue(flags.getStringFeature("a_feature_flag").isPresent(), "Flag should have been present");

        flags.removeFeature("a_feature_flag");
        assertFalse(flags.getStringFeature("a_feature_flag").isPresent(), "Flag should not be present");
    }
}