package gov.cms.dpc.common.hibernate;

import io.dropwizard.db.DataSourceFactory;

public interface IDPCAuthDatabase {
    DataSourceFactory getAuthDatabase();
}
