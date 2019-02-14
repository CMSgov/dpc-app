package gov.cms.dpc.attribution.engine;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import gov.cms.dpc.attribution.engine.AttributionEngine;
import gov.cms.dpc.attribution.engine.AttributionSeeder;
import gov.cms.dpc.attribution.engine.LocalAttributionEngine;
import gov.cms.dpc.attribution.engine.TestSeeder;

public class AttributionEngineModule extends PrivateModule {

    public AttributionEngineModule() {
        // Not used
    }

    @Override
    protected void configure() {
        bind(AttributionEngine.class)
                .to(LocalAttributionEngine.class)
                .in(Scopes.SINGLETON);

        bind(AttributionSeeder.class)
                .to(TestSeeder.class )
                .in(Scopes.SINGLETON);

        expose(AttributionEngine.class);
    }
}
