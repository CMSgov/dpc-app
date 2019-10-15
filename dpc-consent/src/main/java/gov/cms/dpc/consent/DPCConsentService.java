package gov.cms.dpc.consent;

import com.codahale.metrics.jersey2.InstrumentedResourceMethodApplicationListener;
import gov.cms.dpc.common.utils.EnvironmentParser;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import liquibase.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;


public class DPCConsentService extends Application<DPCConsentConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(DPCConsentService.class);

    public static void main(final String[] args) throws Exception {
    }

    @Override
    public String getName() {
        return "DPC Consent Service";
    }

    @Override
    public void run(DPCConsentConfiguration configuration, Environment environment) throws DatabaseException, SQLException {
        EnvironmentParser.getEnvironment("Consent");
        final var listener = new InstrumentedResourceMethodApplicationListener(environment.metrics());
        environment.jersey().getResourceConfig().register(listener);
    }
}
