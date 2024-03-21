package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import io.dropwizard.core.cli.Command;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * Parent class for CLI {@link Command} which make use of the attribution service
 */
public abstract class AbstractAttributionCommand extends Command {

    protected static final String ATTR_HOSTNAME = "hostname";
    protected final FhirContext ctx;

    protected AbstractAttributionCommand(String name, String description) {
        super(name, description);
        this.ctx = FhirContext.forDstu3();
        this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    }

    @Override
    public void configure(Subparser subparser) {
        addAdditionalOptions(subparser);

        subparser
                .addArgument("--host")
                .dest(ATTR_HOSTNAME)
                .required(true)
                .setDefault("http://localhost:3500/v1")
                .help("Address of the Attribution Service, which handles organization registration");
    }

    public abstract void addAdditionalOptions(Subparser subparser);
}
