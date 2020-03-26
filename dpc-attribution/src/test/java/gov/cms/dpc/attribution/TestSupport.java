package gov.cms.dpc.attribution;

import com.google.inject.Module;
import io.dropwizard.Application;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;

public class TestSupport extends DropwizardTestSupport<DPCAttributionConfiguration> {

    private Module serviceModule;

    public TestSupport(Class<? extends Application<DPCAttributionConfiguration>> applicationClass, @Nullable String configPath, ConfigOverride... configOverrides) {
        super(applicationClass, configPath, configOverrides);
    }

    public void setTestServiceModule(Module serviceModule) {
        this.serviceModule = serviceModule;
    }

    @Override
    public Application<DPCAttributionConfiguration> newApplication() {
        try {
            return applicationClass.getConstructor(Module.class).newInstance(serviceModule);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
