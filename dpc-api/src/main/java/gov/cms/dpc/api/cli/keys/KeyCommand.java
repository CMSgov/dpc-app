package gov.cms.dpc.api.cli.keys;

import gov.cms.dpc.api.cli.AbstractCommandTree;

public class KeyCommand extends AbstractCommandTree {

    public KeyCommand() {
        super("key", "Public key related commands");

        registerSubCommand(new KeyDelete());
        registerSubCommand(new KeyList());
        registerSubCommand(new KeyUpload());
    }
}
