package gov.cms.dpc.api.cli;

import gov.cms.dpc.api.DPCAPIConfiguration;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoCommand extends EnvironmentCommand<DPCAPIConfiguration> {

    private static Logger logger = LoggerFactory.getLogger(DemoCommand.class);

    public DemoCommand(Application<DPCAPIConfiguration> application) {
        super(application, "demo", "Execute the demo client");

    }
    @Override
    protected void run(Environment environment, Namespace namespace, DPCAPIConfiguration configuration) throws Exception {
        logger.debug("Running demo!");
    }
}
