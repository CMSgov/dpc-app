package gov.cms.dpc.common.hibernate.validator;

import gov.cms.dpc.common.annotations.OrganizationId;
import gov.cms.dpc.common.entities.stu3.OrganizationEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

class OrganizationIdValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @MethodSource("stringSource")
    void stu3organizationIdValidatorTest(DPCIdentifierSystem dpcIdentifierSystem, String value, boolean isValid) {
        Stu3TestObject testObject = new Stu3TestObject();
        testObject.id = new OrganizationEntity.OrganizationID(dpcIdentifierSystem, value);
        Assertions.assertEquals(isValid, validator.validate(testObject).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("stringSource")
    void r4OrganizationIdValidatorTest(DPCIdentifierSystem system, String value, boolean isValid) {
        R4TestObject testObject = new R4TestObject();
        testObject.id = new gov.cms.dpc.common.entities.r4.OrganizationEntity.OrganizationID(system, value);
        Assertions.assertEquals(isValid, validator.validate(testObject).isEmpty());
    }

    private static Stream<Arguments> stringSource() {
        return Stream.of(
                Arguments.of(DPCIdentifierSystem.NPPES, "9111bb1115", false),
                Arguments.of(DPCIdentifierSystem.NPPES, "9111111115", true),
                Arguments.of(DPCIdentifierSystem.NPPES, "9111111116", false),
                Arguments.of(DPCIdentifierSystem.NPPES, "8111111117", true),
                Arguments.of(DPCIdentifierSystem.NPPES, "8111111118", false),
                Arguments.of(DPCIdentifierSystem.NPPES, "7111111119", true),
                Arguments.of(DPCIdentifierSystem.NPPES, "7111111110", false),
                Arguments.of(DPCIdentifierSystem.NPPES, "6111111111", true),
                Arguments.of(DPCIdentifierSystem.NPPES, "1111111112", true),
                Arguments.of(DPCIdentifierSystem.NPPES, "1632101113", true),
                Arguments.of(DPCIdentifierSystem.NPPES, "4579310721", true),
                Arguments.of(DPCIdentifierSystem.NPPES, "1234567893", true),
                Arguments.of(DPCIdentifierSystem.NPPES, "1234567894", false),
                Arguments.of(DPCIdentifierSystem.NPPES, "123451", false),
                Arguments.of(DPCIdentifierSystem.NPPES, "12345678921", false),
                Arguments.of(DPCIdentifierSystem.NPPES, null, false),
                Arguments.of(DPCIdentifierSystem.PECOS, "1234567894", true),
                Arguments.of(DPCIdentifierSystem.PECOS, "123451", true),
                Arguments.of(DPCIdentifierSystem.PECOS, "12345678921", true),
                Arguments.of(DPCIdentifierSystem.PECOS, UUID.randomUUID().toString(), true),
                Arguments.of(DPCIdentifierSystem.PECOS, null, false)
                );
    }

    static class R4TestObject {
        @Valid
        @OrganizationId
        private gov.cms.dpc.common.entities.r4.OrganizationEntity.OrganizationID id;
    }

    static class Stu3TestObject {
        @Valid
        @OrganizationId
        private OrganizationEntity.OrganizationID id;
    }
}

