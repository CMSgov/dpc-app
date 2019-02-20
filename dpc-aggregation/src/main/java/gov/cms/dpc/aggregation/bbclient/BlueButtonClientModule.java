package gov.cms.dpc.aggregation.bbclient;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class BlueButtonClientModule extends AbstractModule {

    public BlueButtonClientModule(){

    }

    protected void configure() {
        bind(BlueButtonClient.class).to(DefaultBlueButtonClient.class);
    }
}
