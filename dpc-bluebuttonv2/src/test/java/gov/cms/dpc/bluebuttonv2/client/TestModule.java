package gov.cms.dpc.bluebuttonv2.client;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestModule extends AbstractModule {

    TestModule() {
        // Not used
    }

    @Override
    protected void configure() {
        // Not used
    }

    @Provides
    Config provideTestConfig() {
        return ConfigFactory.load("test.application.conf").getConfig("dpc.aggregation");
    }
}
