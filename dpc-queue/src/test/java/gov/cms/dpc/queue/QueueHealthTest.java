package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.common.hibernate.DPCQueueManagedSessionFactory;
import gov.cms.dpc.queue.exceptions.JobQueueUnhealthy;
import gov.cms.dpc.queue.health.JobQueueHealthCheck;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("rawtypes")
public class QueueHealthTest {

    private Session session = mock(Session.class);
    private final SessionFactory factory = mock(SessionFactory.class);
    private DPCQueueManagedSessionFactory managedSessionFactory = new DPCQueueManagedSessionFactory(factory);
    private NativeQuery query = mock(NativeQuery.class);
    private MetricRegistry metrics = new MetricRegistry();

    @BeforeEach
    void setupQueueDependencies() {
        reset(factory, session, query);

        when(factory.openSession())
                .thenReturn(session);

        when(session.createSQLQuery(Mockito.anyString()))
                .thenReturn(query);
    }

    @Test
    void testHealthyQueue() {
        when(query.getFirstResult())
                .thenReturn(0);

        final DistributedBatchQueue queue = new DistributedBatchQueue(managedSessionFactory, 100, metrics);
        assertDoesNotThrow(() -> queue.assertHealthy(UUID.randomUUID()), "Queue should be healthy");

        // Healthcheck should pass
        final JobQueueHealthCheck jobQueueHealthCheck = new JobQueueHealthCheck(queue, UUID.randomUUID());
        assertTrue(jobQueueHealthCheck.check().isHealthy(), "Should be healthy");
    }

    @Test
    void testUnhealthyQueue() {
        when(query.getFirstResult())
                .thenReturn(2);

        final DistributedBatchQueue queue = new DistributedBatchQueue(managedSessionFactory, 100, metrics);
        assertThrows(JobQueueUnhealthy.class, () -> queue.assertHealthy(UUID.randomUUID()), "Queue should be unhealthy");

        // Healthcheck should pass
        final JobQueueHealthCheck jobQueueHealthCheck = new JobQueueHealthCheck(queue, UUID.randomUUID());
        assertFalse(jobQueueHealthCheck.check().isHealthy(), "Should be unhealthy");
    }
}
