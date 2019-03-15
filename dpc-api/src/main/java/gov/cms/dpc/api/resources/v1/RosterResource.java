package gov.cms.dpc.api.resources.v1;

import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.api.resources.AbstractRosterResource;
import org.hl7.fhir.dstu3.model.Bundle;

import javax.inject.Inject;

public class RosterResource extends AbstractRosterResource {

    private final AttributionEngine attributionEngine;

    @Inject
    public RosterResource(AttributionEngine engine) {
        this.attributionEngine = engine;
    }

    // FIXME(nickrobison): Per the FHIR spec, we're supposed to return a MethodOutcome response, but per DPC-128, that's not happening.
    @Override
    public Bundle submitRoster(Bundle providerBundle) {
        attributionEngine.addAttributionRelationships(providerBundle);

        return providerBundle;
    }
}
