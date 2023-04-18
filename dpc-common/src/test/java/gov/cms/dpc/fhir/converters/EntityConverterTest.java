package gov.cms.dpc.fhir.converters;

import com.google.common.collect.ArrayListMultimap;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.fhir.converters.exceptions.DataTranslationException;
import gov.cms.dpc.fhir.converters.exceptions.FHIRConverterException;
import gov.cms.dpc.fhir.converters.exceptions.MissingConverterException;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
public class EntityConverterTest {

    private FHIREntityConverter converter;

    private EntityConverterTest() {
        // Not used
    }

    @BeforeEach
    void setup() {
        converter = new FHIREntityConverter();
    }

    @Test
    void testSimpleConversion() {
        converter.addConverter(new PatientGenderConverter());

        final Patient patient = new Patient();
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        patient.setId(UUID.randomUUID().toString());

        final PatientGenderConverter.PatientGender patientGender = converter.fromFHIR(PatientGenderConverter.PatientGender.class, patient);
        final Patient patient1 = converter.toFHIR(Patient.class, patientGender);
        assertTrue(patient.equalsDeep(patient1), "conversion should be symmetric");
    }


    @Test
    void testMissingConverter() {
        final MissingConverterException exception = assertThrows(MissingConverterException.class, () -> converter.toFHIR(Patient.class, new PatientEntity()));
        assertAll(() -> assertEquals(PatientEntity.class, exception.getSourceClass(), "Should have patient entity source"),
                () -> assertEquals(Patient.class, exception.getTargetClass(), "Should have patient target class"));
    }

    @Test
    void testMultipleConverters() throws ParseException {
        converter.addConverter(new PatientGenderConverter());
        converter.addConverter(new PatientBirthDateConverter());

        final Date testDate = new SimpleDateFormat("yyyy-MM-dd").parse("1990-01-01");
        final UUID testID = UUID.randomUUID();
        final Patient patient = new Patient();
        patient.setBirthDate(testDate);
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        patient.setId(testID.toString());

        // Test patient gender
        final PatientGenderConverter.PatientGender patientGender = converter.fromFHIR(PatientGenderConverter.PatientGender.class, patient);
        final Patient patient1 = converter.toFHIR(Patient.class, patientGender);

        assertAll(() -> assertEquals(testID, UUID.fromString(patient1.getIdElement().getIdPart()), "Should have correct id"),
                () -> assertNull(patient1.getBirthDate(), "Should not have birthdate"),
                () -> assertEquals(Enumerations.AdministrativeGender.FEMALE, patient1.getGender(), "Should have gender"));

        // Test patient birthdate
        final PatientBirthDateConverter.PatientBirthdate patientBirthdate = converter.fromFHIR(PatientBirthDateConverter.PatientBirthdate.class, patient);
        final Patient patient2 = converter.toFHIR(Patient.class, patientBirthdate);
        assertAll(() -> assertEquals(testID, UUID.fromString(patient2.getIdElement().getIdPart()), "Should have correct ID"),
                () -> assertNull(patient2.getGender(), "Should not have gender"),
                () -> assertEquals(patient2.getBirthDate(), testDate, "Should have correct birthdate"));
    }

    @Test
    void testConversionException() {
        // Throws NPE
        converter.addConverter(new PatientGenderConverter());
        // Throws DataTranslationException
        converter.addConverter(new PatientBirthDateConverter());

        final UUID testID = UUID.randomUUID();
        final Patient patient = new Patient();
        patient.setId(testID.toString());

        final DataTranslationException exception = assertThrows(DataTranslationException.class, () -> converter.fromFHIR(PatientBirthDateConverter.PatientBirthdate.class, patient));
        assertAll(() -> assertEquals(Patient.class, exception.getClazz(), "Should have FHIR class"),
                () -> assertEquals("Birthdate", exception.getElement(), "Should have element"));

        final FHIRConverterException converterException = assertThrows(FHIRConverterException.class, () -> converter.fromFHIR(PatientGenderConverter.PatientGender.class, patient), "Should have generic exception");

        assertEquals(NullPointerException.class, converterException.getCause().getClass(), "Should have NPE");
    }

    @Test
    void testDuplicateConverters() {
        converter.addConverter(new PatientGenderConverter());
        final FHIRConverterException exception = assertThrows(FHIRConverterException.class, () -> converter.addConverter(new PatientGenderConverter()));
        assertEquals("Existing converter for org.hl7.fhir.dstu3.model.Patient and gov.cms.dpc.fhir.converters.EntityConverterTest$PatientGenderConverter$PatientGender", exception.getMessage(), "Should have correct error message");
    }


    static class PatientGenderConverter implements FHIRConverter<Patient, PatientGenderConverter.PatientGender> {

        static class PatientGender {
            UUID id;
            String gender;

            PatientGender() {
                // Not used
            }
        }

        PatientGenderConverter() {
            // Not used
        }

        @Override
        public PatientGender fromFHIR(FHIREntityConverter converter, Patient resource) {
            final PatientGender patientGender = new PatientGender();
            patientGender.id = UUID.fromString(resource.getIdElement().getIdPart());
            patientGender.gender = resource.getGender().toString();

            return patientGender;
        }

        @Override
        public Patient toFHIR(FHIREntityConverter converter, PatientGender entity) {
            final Patient patient = new Patient();
            patient.setGender(Enumerations.AdministrativeGender.valueOf(entity.gender));
            patient.setId(entity.id.toString());

            return patient;
        }

        @Override
        public Class<Patient> getFHIRResource() {
            return Patient.class;
        }

        @Override
        public Class<PatientGender> getJavaClass() {
            return PatientGender.class;
        }
    }

    static class PatientBirthDateConverter implements FHIRConverter<Patient, PatientBirthDateConverter.PatientBirthdate> {

        static class PatientBirthdate {
            UUID id;
            Date birthdate;

            PatientBirthdate() {
                // Not used
            }
        }

        @Override
        public PatientBirthdate fromFHIR(FHIREntityConverter converter, Patient resource) {
            final PatientBirthdate patient = new PatientBirthdate();
            patient.id = UUID.fromString(resource.getIdElement().getIdPart());
            if (resource.getBirthDate() == null) {
                throw new DataTranslationException(Patient.class, "Birthdate", "Must have birthdate");
            }
            patient.birthdate = resource.getBirthDate();

            return patient;
        }

        @Override
        public Patient toFHIR(FHIREntityConverter converter, PatientBirthdate entity) {
            final Patient patient = new Patient();
            patient.setId(entity.id.toString());
            patient.setBirthDate(entity.birthdate);

            return patient;
        }

        @Override
        public Class<Patient> getFHIRResource() {
            return Patient.class;
        }

        @Override
        public Class<PatientBirthdate> getJavaClass() {
            return PatientBirthdate.class;
        }
    }
}
