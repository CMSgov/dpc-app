package gov.cms.dpc.queue;

import gov.cms.dpc.queue.exceptions.JobQueueUnhealthy;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.NodesGroup;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class QueueHealthTest {

    private RedissonClient client = mock(RedissonClient.class);
    private Session session = mock(Session.class);
    private NodesGroup nGroup = mock(NodesGroup.class);
    private NativeQuery query = mock(NativeQuery.class);

    @BeforeEach
    void setupQueueDependencies() {
        reset(session, client, nGroup, query);
        // Setup the mocks to return ok
        when(client.getNodesGroup())
                .thenReturn(nGroup);

        when(session.createSQLQuery(Mockito.anyString()))
                .thenReturn(query);
    }


    @Test
    void testHealthyQueue() {
        when(nGroup.pingAll())
                .thenReturn(true);

        when(query.getFirstResult())
                .thenReturn(1);

        final DistributedQueue queue = new DistributedQueue(client, session, "SELECT 1 from job_queue");
        assertDoesNotThrow(queue::isHealthy, "Queue should be healthy");
    }

    @Test
    void testRedisTimeout() {
        when(nGroup.pingAll())
                .then(answer -> {
                    throw new RedisTimeoutException("");
                });

        final DistributedQueue queue = new DistributedQueue(client, session, "SELECT 1 from job_queue");
        final JobQueueUnhealthy unhealthy = assertThrows(JobQueueUnhealthy.class, queue::isHealthy, "Queue should fail due to redis");
        assertEquals(RedisTimeoutException.class, unhealthy.getCause().getClass(), "Should have thrown timeout exception");
    }

    @Test
    void testRedisNodeFailure() {
        when(nGroup.pingAll())
                .thenReturn(false);

        final DistributedQueue queue = new DistributedQueue(client, session, "SELECT 1 from job_queue");
        final JobQueueUnhealthy unhealthy = assertThrows(JobQueueUnhealthy.class, queue::isHealthy, "Queue should fail due to redis");
        assertNotEquals(RedisTimeoutException.class, unhealthy.getCause().getClass(), "Should not have thrown timeout exception");
    }
}
