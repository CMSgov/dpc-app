package gov.cms.dpc.api.cli;

import gov.cms.dpc.api.DPCAPIConfiguration;
import io.dropwizard.cli.Command;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.util.SortedMap;
import java.util.TreeMap;

public class OrganizationCommand extends ConfiguredCommand<DPCAPIConfiguration> {
    private static final String COMMAND_NAME_ATTR = "subcommand";
    private final SortedMap<String, Command> subcommands;

    public OrganizationCommand() {
        super("organization", "Organization related commands");
        this.subcommands = new TreeMap<>();

        // Register subcommands
        registerSubCommand(new OrgListCommand());
        registerSubCommand(new OrgDeleteCommand());
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
    protected void run(Bootstrap<DPCAPIConfiguration> bootstrap, Namespace namespace, DPCAPIConfiguration configuration) throws Exception {
        final Command command = subcommands.get(namespace.getString(COMMAND_NAME_ATTR));
        command.run(bootstrap, namespace);
    }

    private void registerSubCommand(Command command) {
        subcommands.put(command.getName(), command);
    }
}
