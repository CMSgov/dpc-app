package gov.cms.dpc.consent.cli;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.consent.DPCConsentConfiguration;
import io.dropwizard.core.cli.Command;
import io.dropwizard.core.cli.ConfiguredCommand;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * Parent class for CLI {@link Command} which make use of the attribution service
 */
public abstract class ConsentCommand extends ConfiguredCommand<DPCConsentConfiguration> {

    protected static final String ATTRIBUTION_HOST = "attribution_host";
    protected final FhirContext ctx;

    protected ConsentCommand(String name, String description) {
        super(name, description);
        this.ctx = FhirContext.forDstu3();
//        this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    }

    @Override
    public void configure(Subparser subparser) {
        addAdditionalOptions(subparser);

        subparser
                .addArgument("--host")
                .dest(ATTRIBUTION_HOST)
                .required(true)
                .setDefault("http://localhost:3500/v1")
                .help("URL of the Attribution Service (used to verify ids)");
    }

    public abstract void addAdditionalOptions(Subparser subparser);
}