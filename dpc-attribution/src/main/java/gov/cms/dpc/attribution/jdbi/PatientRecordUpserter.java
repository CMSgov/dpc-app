package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.dao.tables.records.PatientsRecord;
import org.jooq.DSLContext;
import org.jooq.TableField;

import java.util.Collections;
import java.util.List;

import static gov.cms.dpc.attribution.dao.tables.Patients.PATIENTS;

/**
 * Implementation of {@link AbstractRecordUpserter}, specialized for {@link PatientsRecord}
 */
public class PatientRecordUpserter extends AbstractRecordUpserter<PatientsRecord> {

    PatientRecordUpserter(DSLContext ctx, PatientsRecord record) {
        super(ctx, record);
    }

    @Override
    List<TableField<PatientsRecord, ?>> getConflictFields() {
        return Collections.singletonList(PATIENTS.BENEFICIARY_ID);
    }

    @Override
    List<TableField<PatientsRecord, ?>> getExcludedFields() {
        return Collections.singletonList(PATIENTS.ID);
    }

    @Override
    List<TableField<PatientsRecord, ?>> getReturnFields() {
        return Collections.singletonList(PATIENTS.ID);
    }
}
