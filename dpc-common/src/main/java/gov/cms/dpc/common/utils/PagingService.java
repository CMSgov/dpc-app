package gov.cms.dpc.common.utils;

import jakarta.inject.Inject;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

import java.util.List;

public class PagingService {

    @Inject
    public PagingService() {
    }

    private String formatURL(String url, int page) {
        return url + "?page=" + page;
    }

    private void addRelationLink(Bundle bundle, String name, String path, int page) {
        bundle.addLink().setRelation(name).setUrl(formatURL(path, page));
    }

    public Bundle convertToBundle(List<Patient> patients) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        for (Patient patient : patients) {
            bundle.addEntry(new Bundle.BundleEntryComponent().setResource(patient));
        }

        return bundle;
    }

    public Bundle handlePaging(List<Patient> patients, int page, String requestPath) {
        Bundle bundle = convertToBundle(patients);
        addRelationLink(bundle, "self", requestPath, page);
        addRelationLink(bundle, "first", requestPath, 1);
        if (page > 1) addRelationLink(bundle, "previous", requestPath, page-1);

        // TODO add a hasNext check - can't always include a "next" link
        addRelationLink(bundle, "next", requestPath, page+1);


        return bundle;
    }
}
