package gov.cms.dpc.aggregation.bbclient;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestModule extends AbstractModule {

    @Provides
    Config provideTestConfig() {
        return ConfigFactory.load();
    }
}
