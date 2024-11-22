package gov.cms.dpc.common.logging;

import com.google.inject.spi.ProvisionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugProvisionListener implements ProvisionListener {
    private static final Logger LOG = LoggerFactory.getLogger(DebugProvisionListener.class);
    private static final boolean TRACE = LOG.isTraceEnabled();

    @Override
    public <T> void onProvision(ProvisionInvocation<T> provision) {
        T instance = provision.provision();

        if(TRACE && provision.getBinding().getKey().getAnnotation() != null)
            LOG.trace("Here is the annotation off the key: " + provision.getBinding().getKey().getAnnotation());
    
        Class<?> type = provision.getBinding().getKey().getTypeLiteral().getRawType();
        LOG.info("Provisioned " + type.getSimpleName() + " from source: " + provision.getBinding().getSource());
         
        // Log the current thread and stack trace
//        logger.info("Current thread: {}", Thread.currentThread().getName());
//        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//        for (int i = 0; i < Math.min(10, stackTrace.length); i++) {
//            logger.info("  at {}", stackTrace[i]);
//        }
        
        if (instance == null) {
            LOG.error("Provision of " + type.getSimpleName() + " resulted in a null instance!");
        } else {
            LOG.info("Successfully provisioned an instance of " + type.getSimpleName());
            if(type.getSimpleName().equals("String"))
                LOG.info("The string value in this case was \"" + instance + "\"");
        }
    }
}
