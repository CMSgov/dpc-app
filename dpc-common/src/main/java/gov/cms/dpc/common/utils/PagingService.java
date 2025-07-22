package gov.cms.dpc.common.utils;

import gov.cms.dpc.common.entities.PatientEntity;
import jakarta.inject.Inject;
import org.hl7.fhir.dstu3.model.Bundle;
import java.util.List;

public class PagingService {

    @Inject
    public PagingService() {
        // TODO document why this constructor is empty
    }

    private String formatURL(String url, int page) {
        return url + "?page=" + page;
    }

    private void addRelationLink(Bundle bundle, String name, String path, int page) {
        bundle.addLink().setRelation(name).setUrl(formatURL(path, page));
    }

    public Bundle handlePaging(List<PatientEntity> patientEntityList, int page, String requestPath) {
//        Bundle bundle = new Bundle();
        Bundle bundle = (Bundle) patientEntityList;
//        bundle.setType(Bundle.BundleType.SEARCHSET);

        addRelationLink(bundle, "self", requestPath, page);
        addRelationLink(bundle, "first", requestPath, 1);
        if (page > 1) addRelationLink(bundle, "previous", requestPath, page-1);

        // TODO add a hasNext check - can't always include a "next" link
        addRelationLink(bundle, "next", requestPath, page+1);

//        // Add patient resources as Bundle entries
//        for (Patient patient : patients) {
//            Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
//            entry.setResource(patient);
//            bundle.addEntry(entry);
//        }

        return bundle;
//        return this.dao.patientSearch(daoSearchQuery)
//                .stream()
//                .map(p -> this.converter.toFHIR(Patient.class, p))
//                .collect(Collectors.toList());
    }
}
