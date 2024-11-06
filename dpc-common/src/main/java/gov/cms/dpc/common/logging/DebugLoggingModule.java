
package gov.cms.dpc.common.logging;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;

public class DebugLoggingModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(DebugLoggingModule.class);

    @Override
    protected void configure() {
        // Log all Validator and ValidatorFactory provisions
        bindListener(Matchers.any(), new ProvisionListener() {
            @Override
            public <T> void onProvision(ProvisionListener.ProvisionInvocation<T> provision) {
                Class<?> type = provision.getBinding().getKey().getTypeLiteral().getRawType();
                if (type == Validator.class || type == ValidatorFactory.class) {
                    logger.info("Attempting to provision {} from source: {}", 
                        type.getSimpleName(),
                        provision.getBinding().getSource());
                    
                    // Log the current thread and stack trace
                    logger.info("Current thread: {}", Thread.currentThread().getName());
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    for (int i = 0; i < Math.min(10, stackTrace.length); i++) {
                        logger.info("  at {}", stackTrace[i]);
                    }
                    
                    // Log environment information
                    logger.info("System Properties:");
                    System.getProperties().forEach((k, v) -> {
                        if (k.toString().contains("java.") || 
                            k.toString().contains("user.") ||
                            k.toString().contains("os.")) {
                            logger.info("  {} = {}", k, v);
                        }
                    });
                }
            }
        });
    }
}