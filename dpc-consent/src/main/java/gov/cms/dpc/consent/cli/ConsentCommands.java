package gov.cms.dpc.consent.cli;

import gov.cms.dpc.consent.DPCConsentConfiguration;
import io.dropwizard.core.cli.Command;
import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.util.NavigableMap;
import java.util.TreeMap;

public class ConsentCommands extends ConfiguredCommand<DPCConsentConfiguration> {
    private static final String COMMAND_NAME_ATTR = "subcommand";
    private final NavigableMap<String, ConsentCommand> subcommands = new TreeMap<>();

    public ConsentCommands() {
        super("consent", "Consent related commands");
        registerSubCommand(new CreateConsentRecord());
    }

    @Override
    public void configure(Subparser subparser) {
        this.subcommands.values().forEach(command -> {
            final Subparser cmdParser = subparser.addSubparsers()
                    .addParser(command.getName())
                    .setDefault(COMMAND_NAME_ATTR, command.getName())
                    .description(command.getDescription());

            command.configure(cmdParser);
        });
    }

    @Override
    protected void run(Bootstrap<DPCConsentConfiguration> bootstrap, Namespace namespace, DPCConsentConfiguration dpcConsentConfiguration) throws Exception {
        final Command command = subcommands.get(namespace.getString(COMMAND_NAME_ATTR));
        command.run(bootstrap, namespace);
    }

    protected void registerSubCommand(ConsentCommand command) {
        subcommands.put(command.getName(), command);
    }

}
