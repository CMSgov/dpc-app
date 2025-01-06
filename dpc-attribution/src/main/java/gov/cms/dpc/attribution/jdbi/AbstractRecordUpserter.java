package gov.cms.dpc.attribution.jdbi;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.UpdatableRecordImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for adding custom handling of {@link org.jooq.Record} insertion or updating.
 * This allows the user to specify how conflicts in the database should be handled.
 * <p>
 * When overriding, the user specifies which columns should be included when determining whether a conflict occurs.
 * The user then specifies which fields to EXCLUDE from updating with the new record.
 * Finally, any specific return fields are listed. If no fields are given, the entire record is returned.
 * <p>
 * If there is no conflicting data in the database, the {@link org.jooq.Record} is inserted without modification.
 *
 * @param <R> - Generic record type which extends {@link UpdatableRecordImpl}.
 */
public abstract class AbstractRecordUpserter<R extends UpdatableRecordImpl<R>> {

    private final DSLContext ctx;
    private final R record;

    AbstractRecordUpserter(DSLContext ctx, R record) {
        this.ctx = ctx;
        this.record = record;
    }

    /**
     * Specify which {@link TableField}s for the given {@link org.jooq.Record} should be considered when determining a conflict occurs.
     * If no fields are specified, then conflict handling will not be implemented.
     *
     * @return - {@link List} of {@link TableField} from the given {@link org.jooq.Record} to consider for conflict detection.
     */
    abstract List<TableField<R, ?>> getConflictFields();

    /**
     * Specify which {@link TableField}s should NOT be updated with the new record values.
     * At the very least, this should always include the record's primary key.
     *
     * @return - {@link List} of {@link TableField} from the given {@link org.jooq.Record} to exclude from updating with the new values.
     */
    abstract List<TableField<R, ?>> getExcludedFields();


    /**
     * Specify which {@link TableField}s should be returned from the database.
     * If no fields are specified, the entire {@link org.jooq.Record} is returned.
     *
     * @return - {@link List} of {@link TableField} from the given {@link org.jooq.Record} to return after the upsert
     */
    abstract List<TableField<R, ?>> getReturnFields();

    /**
     * Return the underlying record
     *
     * @return - {@link R} record to be upserted
     */
    public R getRecord() {
        return this.record;
    }

    /**
     * {@link Map} of {@link String} {@link Object} values which represent the {@link TableField} and values to update for the underlying record.
     * This always includes the values returned by {@link AbstractRecordUpserter#getExcludedFields()}, and optionally includes the values from {@link AbstractRecordUpserter#getConflictFields()} as well.
     *
     * @param excludeConflictFields - {@code true} exclude conflicting fields from the update. {@code false} update conflicting fields.
     * @return - {@link Map} of {@link String} {@link Object} values which will be updated when a conflict occurs.
     */
    public Map<String, Object> getUpdateMap(boolean excludeConflictFields) {
        final Map<String, Object> recordMap = this.record.intoMap();
        this.excludeMapFields(recordMap, getExcludedFields());

        // Always exclude primary keys

        if (excludeConflictFields) {
            this.excludeMapFields(recordMap, getConflictFields());
        }

        return recordMap;
    }

    /**
     * Upsert the record, using the {@link Map} returned by {@link AbstractRecordUpserter#getUpdateMap(boolean)}.
     * This defaults to excluding conflict fields from the update.
     * <p>
     * This also defaults to returning a {@link org.jooq.Record} which only includes the fields specified by {@link AbstractRecordUpserter#getReturnFields()}.
     *
     * @return - {@link R} containing only the fields specified by {@link AbstractRecordUpserter#getReturnFields()}
     */
    public R upsert() {
        return upsert(getReturnFields(), true);
    }

    /**
     * Upsert the record, using the {@link Map} returned by {@link AbstractRecordUpserter#getUpdateMap(boolean)}.
     * Allows the user to specify whether to exclude conflict fields from the update.
     *
     * @param returnFields          - {@link Collection} of {@link TableField} which specifies which values to return from the database
     * @param excludeConflictFields - {@code true} exclude conflicting fields from the update. {@code false} update conflicting fields.
     * @return - {@link R} containing only the fields specified by {@code returnFields}
     */
    public R upsert(Collection<TableField<R, ?>> returnFields, boolean excludeConflictFields) {
        var insertStep = ctx.insertInto(record.getTable())
                .set(record)
                .onConflict(getConflictFields())
                .doUpdate()
                .set(getUpdateMap(excludeConflictFields));

        if (returnFields.isEmpty()) {
            return insertStep
                    .returning()
                    .fetchOne();
        } else {
            return insertStep
                    .returning(returnFields)
                    .fetchOne();
        }
    }

    private void excludeMapFields(Map<String, Object> recordMap, List<TableField<R, ?>> fields) {
        fields.stream()
                .map(Field::getName)
                .forEach(recordMap::remove);
    }
}
