package gov.cms.dpc.attribution;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

public class AttributionModule extends PrivateModule {

    public AttributionModule() {
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
