package gov.cms.dpc.common.hibernate.auth;

import io.dropwizard.db.DataSourceFactory;

public interface IDPCAuthDatabase {
    DataSourceFactory getAuthDatabase();
}
