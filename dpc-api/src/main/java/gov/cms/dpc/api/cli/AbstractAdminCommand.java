package gov.cms.dpc.api.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.cli.Command;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * Parent class for CLI {@link Command} which make use of the admin tasks in the API service
 */
public abstract class AbstractAdminCommand extends Command {

    protected static final String API_HOSTNAME = "hostname";
    protected final FhirContext ctx;
    protected final ObjectMapper mapper;

    protected AbstractAdminCommand(String name, String description) {
        super(name, description);
        this.ctx = FhirContext.forDstu3();
        this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        this.mapper = new ObjectMapper();
    }

    @Override
    public void configure(Subparser subparser) {
        addAdditionalOptions(subparser);

        subparser
                .addArgument("--host")
                .dest(API_HOSTNAME)
                .setDefault("http://localhost:9900/tasks")
                .help("Address of the API Service, which handles organization registration (Must include /tasks endpoint)");
    }

    public abstract void addAdditionalOptions(Subparser subparser);
}
