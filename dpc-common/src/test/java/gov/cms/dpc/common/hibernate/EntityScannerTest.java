package gov.cms.dpc.common.hibernate;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableList;
import gov.cms.dpc.common.entities.r4.*;
import gov.cms.dpc.common.hibernate.attribution.DPCR4HibernateBundle;
import gov.cms.dpc.common.hibernate.attribution.DPCStu3HibernateBundle;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityScannerTest {

    @Test
    void testStu3ApplicationEntities() {
        assertApplicationEntities(DPCStu3HibernateBundle.PREFIX_STRING, List.of(
                gov.cms.dpc.common.entities.stu3.AttributionRelationship.class,
                gov.cms.dpc.common.entities.stu3.ContactEntity.class,
                gov.cms.dpc.common.entities.stu3.ContactPointEntity.class,
                gov.cms.dpc.common.entities.stu3.OrganizationEntity.class,
                gov.cms.dpc.common.entities.stu3.PatientEntity.class,
                gov.cms.dpc.common.entities.stu3.ProviderEntity.class,
                gov.cms.dpc.common.entities.stu3.RosterEntity.class
        ));
    }

    @Test
    void testR4ApplicationEntities() {
        assertApplicationEntities(DPCR4HibernateBundle.PREFIX_STRING, List.of(
                AttributionRelationship.class,
                ContactEntity.class,
                ContactPointEntity.class,
                OrganizationEntity.class,
                PatientEntity.class,
                ProviderEntity.class,
                RosterEntity.class
        ));
    }

    private void assertApplicationEntities(String prefix, List<Class<?>> entityList) {
        Logger logger = (Logger) LoggerFactory.getLogger(EntityScanner.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            ImmutableList<Class<?>> entities = EntityScanner.applicationEntities(prefix, List.of());
            assertTrue(isEqualCollection(entityList, entities));
            assertEquals(String.format("Scanning %s for Hibernate entities", prefix), listAppender.list.get(0).getFormattedMessage());

            List<String> logMessages = new ArrayList<>();
            listAppender.list.stream().skip(2).forEach(logEvent -> logMessages.add(logEvent.getFormattedMessage()));
            entityList.forEach(entity -> assertTrue(logMessages.contains(String.format("Registered %s.", entity.getName()))));
        } finally {
            logger.detachAppender(listAppender);
            listAppender.stop();
        }
    }
}
