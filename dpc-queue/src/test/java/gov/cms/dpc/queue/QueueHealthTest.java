package gov.cms.dpc.queue;

import com.codahale.metrics.MetricRegistry;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.redisson.api.Node;
import org.redisson.api.NodesGroup;
import org.redisson.api.RedissonClient;

import static org.mockito.Mockito.*;

@SuppressWarnings("rawtypes")
public class QueueHealthTest {

    private RedissonClient client = mock(RedissonClient.class);
    private Session session = mock(Session.class);
    private final SessionFactory factory = mock(SessionFactory.class);
    private DPCManagedSessionFactory managedSessionFactory = new DPCManagedSessionFactory(factory);
    @SuppressWarnings("unchecked")
    private NodesGroup<Node> nGroup = mock(NodesGroup.class);
    private NativeQuery query = mock(NativeQuery.class);
    private MetricRegistry metrics = new MetricRegistry();

    @BeforeEach
    void setupQueueDependencies() {
        reset(factory, session, client, nGroup, query);
        // Setup the mocks to return ok
        when(client.getNodesGroup())
                .thenReturn(nGroup);

        when(factory.openSession())
                .thenReturn(session);

        when(session.createSQLQuery(Mockito.anyString()))
                .thenReturn(query);
    }


//    @Test
//    void testHealthyQueue() {
//        when(nGroup.pingAll())
//                .thenReturn(true);
//
//        when(query.getFirstResult())
//                .thenReturn(1);
//
//        final DistributedQueue queue = new DistributedQueue(client, managedSessionFactory, "SELECT 1 from job_queue", metrics);
//        assertDoesNotThrow(queue::assertHealthy, "Queue should be healthy");
//
//        // Healthcheck should pass
//        final JobQueueHealthCheck jobQueueHealthCheck = new JobQueueHealthCheck(queue);
//        assertTrue(jobQueueHealthCheck.check().isHealthy(), "Should be healthy");
//    }
//
//    @Test
//    void testRedisTimeout() {
//        when(nGroup.pingAll())
//                .then(answer -> {
//                    throw new RedisTimeoutException("");
//                });
//
//        final DistributedQueue queue = new DistributedQueue(client, managedSessionFactory, "SELECT 1 from job_queue", metrics);
//        final JobQueueUnhealthy unhealthy = assertThrows(JobQueueUnhealthy.class, queue::assertHealthy, "Queue should fail due to redis");
//        assertEquals(RedisTimeoutException.class, unhealthy.getCause().getClass(), "Should have thrown timeout exception");
//
//        // Healthcheck should fail
//        final HealthCheck.Result result = new JobQueueHealthCheck(queue).check();
//        assertAll(() -> assertFalse(result.isHealthy(), "Should not be healthy"),
//                () -> assertEquals(DistributedQueue.REDIS_UNHEALTHY, result.getMessage(), "Message should be propagated"));
//    }
//
//    @Test
//    void testRedisNodeFailure() {
//        when(nGroup.pingAll())
//                .thenReturn(false);
//
//        final DistributedQueue queue = new DistributedQueue(client, managedSessionFactory, "SELECT 1 from job_queue", metrics);
//        final JobQueueUnhealthy unhealthy = assertThrows(JobQueueUnhealthy.class, queue::assertHealthy, "Queue should fail due to redis");
//        assertNotEquals(RedisTimeoutException.class, unhealthy.getCause().getClass(), "Should not have thrown timeout exception");
//
//        // Healthcheck should fail
//        final HealthCheck.Result result = new JobQueueHealthCheck(queue).check();
//        assertAll(() -> assertFalse(result.isHealthy(), "Should not be healthy"),
//                () -> assertEquals(DistributedQueue.REDIS_UNHEALTHY, result.getMessage(), "Message should be propagated"));
//    }
}
