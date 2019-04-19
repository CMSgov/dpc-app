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
        return Collections.singletonList(PROVIDERS.PROVIDER_ID);
    }

    @Override
    List<TableField<ProvidersRecord, ?>> getExcludedFields() {
        return Collections.singletonList(PROVIDERS.ID);
    }

    @Override
    List<TableField<ProvidersRecord, ?>> getReturnFields() {
        return Collections.singletonList(PROVIDERS.ID);
    }
}
