package gov.cms.dpc.aggregation.bbclient;

import com.google.inject.AbstractModule;

public class BlueButtonClientModule extends AbstractModule {

    public BlueButtonClientModule(){

    }

    protected void configure() {
        bind(BlueButtonClient.class).toProvider(BlueButtonClientProvider.class);
    }
}
