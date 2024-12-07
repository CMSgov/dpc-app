
package gov.cms.dpc.common.logging;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

public class DebugLoggingModule extends AbstractModule {
    
     @Override
    protected void configure() {
        bindListener(Matchers.any(), new DebugProvisionListener());
    }
}