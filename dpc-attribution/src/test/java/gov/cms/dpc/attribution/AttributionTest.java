package gov.cms.dpc.attribution;

import com.google.inject.Guice;
import com.google.inject.Injector;
import gov.cms.dpc.attribution.engine.AttributionEngine;
import gov.cms.dpc.attribution.engine.AttributionEngineModule;
import gov.cms.dpc.attribution.engine.TestSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AttributionTest {

    private static final Injector injector = Guice.createInjector(new AttributionEngineModule());
    private AttributionEngine engine;

    @BeforeEach
    public void setupTest() {
        this.engine = injector.getInstance(AttributionEngine.class);
        final TestSeeder testSeeder = new TestSeeder(engine);
        testSeeder.seedAttribution();
    }

    // This is mostly a useless test, it's just to get some things passing, for now.
    @Test
    public void testAttributionSeeding() {
        assertEquals(50, engine.getAttributedBeneficiaries("0c527d2e-2e8a-4808-b11d-0fa06baf8254").size(), "Should have patients");
        assertTrue(engine.getAttributedBeneficiaries("0c527d2e-2e8a-4808-b11d-0fa06baf8259").isEmpty(), "Should not have patients");

        engine.removeAttributionRelationship("0c527d2e-2e8a-4808-b11d-0fa06baf8254", "19990000002901");
        assertEquals(49, engine.getAttributedBeneficiaries("0c527d2e-2e8a-4808-b11d-0fa06baf8254").size(), "Should have less patients");

        engine.addAttributionRelationship("0c527d2e-2e8a-4808-b11d-0fa06baf8254", "19990000002901");
        assertEquals(50, engine.getAttributedBeneficiaries("0c527d2e-2e8a-4808-b11d-0fa06baf8254").size(), "Should have original patients");

        engine.addAttributionRelationship("0c527d2e-2e8a-4808-b11d-0fa06baf8254", "19990000002901");
        assertEquals(50, engine.getAttributedBeneficiaries("0c527d2e-2e8a-4808-b11d-0fa06baf8254").size(), "Should have original patients");
    }
}
