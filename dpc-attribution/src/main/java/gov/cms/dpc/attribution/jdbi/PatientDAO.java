package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.hibernate.DPCManagedSessionFactory;
import io.dropwizard.hibernate.AbstractDAO;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

public class PatientDAO extends AbstractDAO<PatientEntity> {

    @Inject
    public PatientDAO(DPCManagedSessionFactory factory) {
        super(factory.getSessionFactory());
    }

    public PatientEntity persistPatient(PatientEntity patient) {
        return this.persist(patient);
    }

    public Optional<PatientEntity> getPatient(UUID patientID) {
        return Optional.ofNullable(get(patientID));
    }


}
