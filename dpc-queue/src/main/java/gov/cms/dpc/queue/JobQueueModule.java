package gov.cms.dpc.queue;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class JobQueueModule extends AbstractModule {

    private final boolean inMemory;

    public JobQueueModule() {
        this.inMemory = true;
    }

    public JobQueueModule(boolean inMemory) {
        this.inMemory = inMemory;
    }

    @Override
    protected void configure() {
        // Manually bind to the Memory Queue, as a Singleton
        bind(JobQueue.class)
                .to(MemoryQueue.class)
                .in(Scopes.SINGLETON);
    }
}
