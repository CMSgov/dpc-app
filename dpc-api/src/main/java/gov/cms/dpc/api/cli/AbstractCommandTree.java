package gov.cms.dpc.api.cli;

import gov.cms.dpc.api.DPCAPIConfiguration;
import io.dropwizard.core.cli.Command;
import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.util.NavigableMap;
import java.util.TreeMap;

public class AbstractCommandTree extends ConfiguredCommand<DPCAPIConfiguration> {
    private static final String COMMAND_NAME_ATTR = "subcommand";
    private final NavigableMap<String, Command> subcommands;

    protected AbstractCommandTree(String name, String description) {
        super(name, description);
        this.subcommands = new TreeMap<>();
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
    protected void run(Bootstrap<DPCAPIConfiguration> bootstrap, Namespace namespace, DPCAPIConfiguration dpcapiConfiguration) throws Exception {
        final Command command = subcommands.get(namespace.getString(COMMAND_NAME_ATTR));
        command.run(bootstrap, namespace);
    }

    protected void registerSubCommand(Command command) {
        subcommands.put(command.getName(), command);
    }
}
