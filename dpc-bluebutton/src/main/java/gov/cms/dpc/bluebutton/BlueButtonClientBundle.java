package gov.cms.dpc.bluebutton;

import gov.cms.dpc.bluebutton.config.BlueButtonBundleConfiguration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class BlueButtonClientBundle implements ConfiguredBundle<BlueButtonBundleConfiguration> {


    @Override
    public void run(BlueButtonBundleConfiguration configuration, Environment environment) throws Exception {

    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }
}
