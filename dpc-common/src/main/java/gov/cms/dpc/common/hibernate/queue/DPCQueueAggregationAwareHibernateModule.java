package gov.cms.dpc.common.hibernate.queue;

import com.google.inject.Provides;
import gov.cms.dpc.common.utils.CurrentEngineState;
import io.dropwizard.core.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

public class DPCQueueAggregationAwareHibernateModule<T extends Configuration & IDPCQueueDatabase> extends DPCQueueHibernateModule<T> {
	public DPCQueueAggregationAwareHibernateModule(DPCQueueHibernateBundle<T> hibernate) {
		super(hibernate);
	}

	@Provides
	@Singleton
	@Inject
	DPCQueueAggregationAwareManagedSessionFactory getSessionFactory(CurrentEngineState state) {
		return new DPCQueueAggregationAwareManagedSessionFactory(this.hibernate.getSessionFactory(), state);
	}

	@Provides
	@Singleton
	CurrentEngineState getCurrentEngineState() {
		return new CurrentEngineState();
	}
}
