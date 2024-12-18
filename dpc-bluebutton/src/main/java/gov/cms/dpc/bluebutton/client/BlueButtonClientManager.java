package gov.cms.dpc.bluebutton.client;

import io.dropwizard.lifecycle.Managed;
import jakarta.inject.Inject;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BlueButtonClientManager implements Managed {

    private final BlueButtonClient client;

    @Inject
    public BlueButtonClientManager(BlueButtonClient client) {
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

    public BlueButtonClient getClient() {
        return this.client;
    }
}
