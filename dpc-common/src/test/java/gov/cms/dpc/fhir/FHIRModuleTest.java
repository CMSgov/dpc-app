package gov.cms.dpc.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.configuration.IDPCFHIRConfiguration;
import gov.cms.dpc.fhir.dropwizard.handlers.r4.FHIRHandler;
import io.dropwizard.core.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class FHIRModuleTest {

    @Test
    void providesNamedContextsForEachVersion() {
        // Make sure we can get stu3 and r4 contexts from the FHIRModule
        Injector injector = Guice.createInjector(new FHIRModule<TestConfiguration>() {
            @Override
            public void configure() { bind(gov.cms.dpc.fhir.dropwizard.handlers.stu3.FHIRHandler.class); }
        });
        Key<FhirContext> stu3Key = Key.get(FhirContext.class, Names.named("stu3"));
        Key<FhirContext> r4Key = Key.get(FhirContext.class, Names.named("r4"));
        FhirContext stu3 = injector.getInstance(stu3Key);
        FhirContext r4 = injector.getInstance(r4Key);

        assertEquals(FhirVersionEnum.DSTU3, stu3.getVersion().getVersion());
        assertEquals(FhirVersionEnum.R4, r4.getVersion().getVersion());
        assertSame(stu3, injector.getInstance(stu3Key));
        assertSame(r4, injector.getInstance(r4Key));
        assertDoesNotThrow(() -> injector.getInstance(gov.cms.dpc.fhir.dropwizard.handlers.stu3.FHIRHandler.class));
        assertDoesNotThrow(() -> injector.getInstance(FHIRHandler.class));
    }

    private static class TestConfiguration extends Configuration implements IDPCFHIRConfiguration {
        private DPCFHIRConfiguration fhirConfiguration;

        @Override
        public DPCFHIRConfiguration getFHIRConfiguration() {
            return fhirConfiguration;
        }

        @Override
        public void setFHIRConfiguration(DPCFHIRConfiguration config) {
            fhirConfiguration = config;
        }
    }
}
