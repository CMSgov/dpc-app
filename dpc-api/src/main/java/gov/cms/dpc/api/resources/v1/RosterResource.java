package gov.cms.dpc.api.resources.v1;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import gov.cms.dpc.common.interfaces.AttributionEngine;
import gov.cms.dpc.api.resources.AbstractRosterResource;
import org.hl7.fhir.dstu3.model.Bundle;

import javax.inject.Inject;
import java.util.UUID;

public class RosterResource extends AbstractRosterResource {

    private final AttributionEngine attributionEngine;

    @Inject
    public RosterResource(AttributionEngine engine) {
        this.attributionEngine = engine;
    }

    // TODO(nickrobison): The FHIR spec says we're supposed to return a MethodOutcome response, but per DPC-128, that's not happening.
    // TODO(nickrobison): Perform FHIR input validation
    @Override
    @Timed
    @ExceptionMetered
    public Bundle submitRoster(Bundle providerBundle) {
        // FIXME(nickrobison): Remove this!
        final UUID organizationID = UUID.randomUUID();
        attributionEngine.addAttributionRelationships(providerBundle, organizationID);

        return providerBundle;
    }
}
