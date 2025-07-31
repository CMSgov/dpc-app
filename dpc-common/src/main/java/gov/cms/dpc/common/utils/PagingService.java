package gov.cms.dpc.common.utils;

import com.google.inject.name.Named;
import jakarta.inject.Inject;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseBundle;

import java.util.List;

public class PagingService {
    private final String publicURL;

    @Inject
    public PagingService(@Named("publicURL") String publicURL) {
        this.publicURL = publicURL;
    }

    private String formatURL(String url, int count, int offset) {
        String dpcApiBaseUrl = this.publicURL;
        return dpcApiBaseUrl + url + "?_count=" + count + "&_offset=" + offset;
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
        addRelationLink(bundle, IBaseBundle.LINK_SELF, requestPath, count, offset);
        addRelationLink(bundle, "first", requestPath, count, 0);
        if (offset > count && !patients.isEmpty()) addRelationLink(bundle, IBaseBundle.LINK_PREV, requestPath, count, offset-count);
        if (hasNext) addRelationLink(bundle, IBaseBundle.LINK_NEXT, requestPath, count, offset+count);


        return bundle;
    }
}
