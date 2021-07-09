package gov.cms.dpc.bluebuttonv2.client;

import io.dropwizard.lifecycle.Managed;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

@Provider
public class BlueButtonClientManagerV2 implements Managed {

    private final BlueButtonClientV2 client;

    @Inject
    public BlueButtonClientManagerV2(BlueButtonClientV2 client) {
        this.client = client;
    }

    @Override
    public void start() throws Exception {
        // Not used
    }

    @Override
    public void stop() throws Exception {
        // Not used
    }

    public BlueButtonClientV2 getClient() {
        return this.client;
    }
}
