package gov.cms.dpc.common.utils;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

import java.util.List;

public class PagingService {

    private String formatURL(String url, int count, int offset) {
        return url + "?_count=" + count + "&_offset=" + offset;
    }

    private void addRelationLink(Bundle bundle, String name, String path, int count, int offset) {
        bundle.addLink().setRelation(name).setUrl(formatURL(path, count, offset));
    }

    public Bundle convertToBundle(List<Patient> patients) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        for (Patient patient : patients) {
            bundle.addEntry(new Bundle.BundleEntryComponent().setResource(patient));
        }

        return bundle;
    }

    public Bundle convertToSummaryBundle(int total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(total);
        return bundle;
    }

    public Bundle handlePagingLinks(List<Patient> patients, int count, int offset, String requestPath, boolean hasNext) {
        Bundle bundle = convertToBundle(patients);
        addRelationLink(bundle, "self", requestPath, count, offset);
        addRelationLink(bundle, "first", requestPath, count, 0);
        if (offset > count && !patients.isEmpty()) addRelationLink(bundle, "previous", requestPath, count, offset-count);
        if (hasNext) addRelationLink(bundle, "next", requestPath, count, offset+count);


        return bundle;
    }
}
