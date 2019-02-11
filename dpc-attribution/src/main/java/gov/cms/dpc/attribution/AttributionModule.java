package gov.cms.dpc.attribution;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class AttributionModule extends AbstractModule {

    AttributionModule() {
        // Not used
    }

    @Override
    protected void configure() {
        bind(AttributionEngine.class)
                .to(LocalAttributionEngine.class)
                .in(Scopes.SINGLETON);
    }
}
