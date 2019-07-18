package gov.cms.dpc.fhir.converters.entities;

import gov.cms.dpc.common.entities.ProviderRoleEntity;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Reference;

public class ProviderRoleEntityConverter {

    private ProviderRoleEntityConverter() {
        // Not used
    }

    public static PractitionerRole convert(ProviderRoleEntity entity) {
        final PractitionerRole practitionerRole = new PractitionerRole();

        practitionerRole.setId(new IdType("PractitionerRole", entity.getRoleID().toString()));
        practitionerRole.setOrganization(new Reference(new IdType("Organization", entity.getOrganization().getId().toString())));
        practitionerRole.setPractitioner(new Reference(new IdType("Practitioner", entity.getProvider().getProviderID().toString())));

        return practitionerRole;
    }
}
