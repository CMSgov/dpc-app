package gov.cms.dpc.common.hibernate.attribution;

import io.dropwizard.db.DataSourceFactory;

public interface IDPCDatabase {
    DataSourceFactory getDatabase();
}
