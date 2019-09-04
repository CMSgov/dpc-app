package gov.cms.dpc.api.cli.organizations;

import gov.cms.dpc.api.cli.AbstractCommandTree;

public class OrganizationCommand extends AbstractCommandTree {

    public OrganizationCommand() {
        super("organization", "Organization related commands");

        // Register subcommands
        registerSubCommand(new OrganizationList());
        registerSubCommand(new OrganizationDelete());
        registerSubCommand(new OrganizationRegistration());
    }
}
