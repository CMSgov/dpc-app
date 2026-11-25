package gov.cms.dpc.common.utils.r4;

import gov.cms.dpc.fhir.dropwizard.handlers.r4.BundleHandler;
import jakarta.ws.rs.core.UriBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseBundle;

import java.util.List;

public class PagingService {
    private String formatURL(String requestPath, int count, int offset, String patientMBI) {
        String url = requestPath + "?_count=" + count + "&_offset=" + offset;
        if (patientMBI != null && !patientMBI.isEmpty()) {
            return UriBuilder.fromUri(url).queryParam(Patient.SP_IDENTIFIER, patientMBI).build().toString();
        }
        return url;
    }

    private void addRelationLink(Bundle bundle, String name, String path, int count, int offset, String patientMBI) {
        bundle.addLink().setRelation(name).setUrl(formatURL(path, count, offset, patientMBI));
    }

    public Bundle handlePagingLinks(List<Patient> patients, int count, int offset,
                                    String requestPath, String patientMBI, boolean hasNext) {
        Bundle bundle = BundleHandler.convertToBundle(patients);
        addRelationLink(bundle, IBaseBundle.LINK_SELF, requestPath, count, offset, patientMBI);
        addRelationLink(bundle, "first", requestPath, count, 0, patientMBI);
        if (offset > count - 1 && !patients.isEmpty()) {
            addRelationLink(bundle, IBaseBundle.LINK_PREV, requestPath, count, offset - count, patientMBI);
        }
        if (hasNext) {
            addRelationLink(bundle, IBaseBundle.LINK_NEXT, requestPath, count, offset + count, patientMBI);
        }


        return bundle;
    }
}
