package gov.cms.dpc.common.hibernate.validator;

import gov.cms.dpc.common.annotations.OrganizationId;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class OrganizationIdValidator implements ConstraintValidator<OrganizationId, OrganizationEntity.OrganizationID> {

    public static final String VALIDATION_MESSAGE = "Invalid Organization ID format";

    @Override
    public void initialize(OrganizationId organizationId) {
        //not used
    }

    @Override
    public boolean isValid(OrganizationEntity.OrganizationID id, ConstraintValidatorContext constraintValidatorContext) {
        if (id.getSystem() != DPCIdentifierSystem.NPPES) {
            return true;
        }

        return NPIUtil.isValidNPI(id.getValue());
    }

}
