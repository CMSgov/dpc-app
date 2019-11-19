package gov.cms.dpc.common.hibernate.consent;

import io.dropwizard.db.DataSourceFactory;

public interface IDPCConsentDatabase {
    DataSourceFactory getConsentDatabase();
}
