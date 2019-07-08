package gov.cms.dpc.fhir.validations.definitions;

import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.StructureDefinition;

import java.util.List;

public class MatchablePatient {

    private MatchablePatient() {
        // Not used
    }

    public static StructureDefinition definition() {

        final StructureDefinition matcheablePatient = new StructureDefinition();
        matcheablePatient.setId("dpc-patient");
        matcheablePatient.setName("Matcheable Patient");
        matcheablePatient.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
        matcheablePatient.setType("Patient");
        matcheablePatient.setBaseDefinition("http://hl7.org/fhir/StructureDefinition/Patient");
        matcheablePatient.setUrl("http://test.gov/patient");
        // Not great, completely wrong?
        matcheablePatient.setDerivation(StructureDefinition.TypeDerivationRule.CONSTRAINT);
        matcheablePatient.setAbstract(false);
        matcheablePatient.setStatus(Enumerations.PublicationStatus.DRAFT);

        final StructureDefinition.StructureDefinitionDifferentialComponent dbDiff = new StructureDefinition.StructureDefinitionDifferentialComponent();

        final ElementDefinition patientElement = new ElementDefinition();
        patientElement.setDefinition("Patient");
        patientElement.setId("Patient");
        patientElement.setPath("Patient");
        patientElement.setMin(1);
        patientElement.setMax("1");

        final ElementDefinition birthDayElement = new ElementDefinition();
        birthDayElement.setDefinition("Birth date");
        birthDayElement.setId("Patient.birthDate");
        birthDayElement.setPath("Patient.birthDate");
        birthDayElement.setMin(1);
//        birthDayElement.setMax("1");

//        dbDiff.addElement(birthDayElement);


        // We need a name as well
        final ElementDefinition nameElement = new ElementDefinition();
        nameElement.setDefinition("Patient name");
        nameElement.setId("Patient.name");
        nameElement.setPath("Patient.name");
        nameElement.setMin(1);

        // First and last
        final ElementDefinition givenElement = new ElementDefinition();
        givenElement.setDefinition("Patient given name");
        givenElement.setId("Patient.name.given");
        givenElement.setPath("Patient.name.given");
        givenElement.setMin(1);

        dbDiff.addElement(nameElement);
        dbDiff.addElement(givenElement);
        dbDiff.addElement(birthDayElement);

        matcheablePatient.setDifferential(dbDiff);


        // Snapshot
//
//        final StructureDefinition.StructureDefinitionSnapshotComponent comp = new StructureDefinition.StructureDefinitionSnapshotComponent();
////
//        // Root element
//        final ElementDefinition rootElement = new ElementDefinition();
//        rootElement.setDefinition("Root def");
//        rootElement.setMin(1);
//        rootElement.setMax("1");
//        rootElement.setId("Patient");
//        rootElement.setPath("Patient");
//        final ElementDefinition.ElementDefinitionBaseComponent rootBase = new ElementDefinition.ElementDefinitionBaseComponent();
//
//        rootBase.setPath("Patient");
//        rootBase.setMin(0);
//        rootBase.setMax("1");
////
//        rootElement.setBase(rootBase);
////
////        // First name
//        final ElementDefinition firstNameDef = new ElementDefinition();
//        firstNameDef.setDefinition("First name");
//        firstNameDef.setId("Patient.name");
//        firstNameDef.setPath("Patient.name");
//        firstNameDef.setMin(1);
//        firstNameDef.setMax("*");
//        firstNameDef.addType().setCode("code");
//        final ElementDefinition.ElementDefinitionBaseComponent bc = new ElementDefinition.ElementDefinitionBaseComponent();
//        bc.setPath("Patient.name");
//        bc.setMin(0);
//        bc.setMax("*");
//        firstNameDef.setBase(bc);
////
////        final ElementDefinition birthDayElement = new ElementDefinition();
////        birthDayElement.setDefinition("Birth date");
////        birthDayElement.setId("Patient.birthDate");
////        birthDayElement.setPath("Patient.birthDate");
////        birthDayElement.setMin(1);
////        birthDayElement.setMax("1");
////
//        final ElementDefinition.ElementDefinitionBaseComponent bbc = new ElementDefinition.ElementDefinitionBaseComponent();
//        bbc.setPath("Patient.birthDate");
//        bbc.setMin(0);
//        bbc.setMax("1");
//        birthDayElement.setBase(bbc);
////
//        comp
//                .addElement(rootElement)
//                .addElement(firstNameDef)
//                .addElement(birthDayElement);
////
////        matcheablePatient.setSnapshot(comp);

        return matcheablePatient;
    }
}
