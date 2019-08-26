package gov.cms.dpc.api.cli.tokens;

import gov.cms.dpc.api.cli.AbstractCommandTree;

public class TokenCommand extends AbstractCommandTree {

    public TokenCommand() {
        super("token", "Token related commands");

        registerSubCommand(new TokenDelete());
        registerSubCommand(new TokenList());
        registerSubCommand(new TokenCreate());
    }
}
