
package gov.cms.dpc.common.logging;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.matcher.Matchers;

public class DebugLoggingModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(DebugLoggingModule.class);

    
     @Override
    protected void configure() {
        bindListener(Matchers.any(), new DebugProvisionListener());
    }
}