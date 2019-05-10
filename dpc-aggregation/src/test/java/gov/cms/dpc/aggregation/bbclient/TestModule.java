package gov.cms.dpc.aggregation.bbclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import gov.cms.dpc.common.annotations.ExportPath;

import java.io.IOException;

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
