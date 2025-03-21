package gov.cms.dpc.common.hibernate;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableList;
import gov.cms.dpc.common.entities.*;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityScannerTest {

    static final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    static final String PREFIX_STRING = "gov.cms.dpc.common.entities";

    @Test
    void testApplicationEntities() {
        Logger logger = (Logger) LoggerFactory.getLogger(EntityScanner.class);
        listAppender.start();
        logger.addAppender(listAppender);

        List<Class<?>> entityList = List.of(
                AttributionRelationship.class,
                ContactEntity.class,
                ContactPointEntity.class,
                OrganizationEntity.class,
                PatientEntity.class,
                ProviderEntity.class,
                RosterEntity.class
        );

        ImmutableList<Class<?>> entities = EntityScanner.applicationEntities(PREFIX_STRING, List.of());
        assertTrue(isEqualCollection(entityList, entities));
        listAppender.list.forEach(logEvent -> System.out.println(logEvent.getFormattedMessage()));
        assertEquals(String.format("Scanning %s for Hibernate entities", PREFIX_STRING), listAppender.list.get(0).getFormattedMessage());

        List<String> logMessages = new ArrayList<>();
        listAppender.list.stream().skip(2).forEach(logEvent -> logMessages.add(logEvent.getFormattedMessage()));
        entityList.forEach(entity -> assertTrue(logMessages.contains(String.format("Registered %s.", entity.getName()))));

        listAppender.stop();
        listAppender.list.clear();
    }
}
