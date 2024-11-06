package gov.cms.dpc.common.logging;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiceLoggingModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(GuiceLoggingModule.class);

    @Override
    protected void configure() {
        // This is where you can log each binding
        Binder binder = binder();
        binder.bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <T> void hear(TypeLiteral<T> typeLiteral, TypeEncounter<T> typeEncounter) {
//                logger.info("Guicey Binding: " + typeLiteral);
            }
        });
    }
}




