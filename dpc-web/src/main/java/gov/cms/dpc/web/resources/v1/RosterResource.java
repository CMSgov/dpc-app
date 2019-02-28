package gov.cms.dpc.web.resources.v1;

import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.web.resources.AbstractRosterResource;
import org.hl7.fhir.dstu3.model.Bundle;

import javax.inject.Inject;

public class RosterResource extends AbstractRosterResource {

    private final AttributionEngine attributionEngine;

    @Inject
    public RosterResource(AttributionEngine engine) {
        this.attributionEngine = engine;
    }

    @Override
    public void submitRoster(Bundle providerBundle) {
        final String providerID = providerBundle.getEntryFirstRep().getResource().getId();
    }
}
