package gov.cms.dpc.common.hibernate.queue;

import io.dropwizard.db.DataSourceFactory;

public interface IDPCQueueDatabase {
    DataSourceFactory getQueueDatabase();
}
