package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.dao.tables.records.ProvidersRecord;
import org.jooq.DSLContext;
import org.jooq.TableField;

import java.util.Collections;
import java.util.List;

import static gov.cms.dpc.attribution.dao.tables.Providers.PROVIDERS;

/**
 * Implementation of {@link AbstractRecordUpserter}, specialized for {@link ProvidersRecord}
 */
public class ProviderRecordUpserter extends AbstractRecordUpserter<ProvidersRecord> {

    public ProviderRecordUpserter(DSLContext ctx, ProvidersRecord record) {
        super(ctx, record);
    }

    @Override
    List<TableField<ProvidersRecord, ?>> getConflictFields() {
        return List.of(PROVIDERS.PROVIDER_ID, PROVIDERS.ORGANIZATION_ID);
    }

    @Override
    List<TableField<ProvidersRecord, ?>> getExcludedFields() {
        return List.of(PROVIDERS.ID, PROVIDERS.PROVIDER_ID, PROVIDERS.ORGANIZATION_ID);
    }

    @Override
    List<TableField<ProvidersRecord, ?>> getReturnFields() {
        return Collections.singletonList(PROVIDERS.ID);
    }
}
