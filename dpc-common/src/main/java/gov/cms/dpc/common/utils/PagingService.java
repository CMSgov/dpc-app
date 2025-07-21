package gov.cms.dpc.common.utils;

import ca.uhn.fhir.rest.gclient.IQuery;
import jakarta.inject.Inject;
import org.hl7.fhir.dstu3.model.Bundle;

public class PagingService {
    private final int defaultCount;

    @Inject
    public PagingService(int defaultCount) {
        this.defaultCount = defaultCount;
    }

    private String formatURL(String url, int page) {
        return url + "?page=" + page;
    }

    private void addRelationLink(Bundle bundle, String name, String path, int page) {
        bundle.addLink().setRelation(name).setUrl(formatURL(path, page));
    }

    public Bundle handlePaging(IQuery<Bundle> request, int count, int page, String requestPath) {
        if (count == -1) {
            count = defaultCount;
        }
        request.offset(count*(page-1));
        request.count(count);
        Bundle resultBundle = request.execute(); // this should call attribution

        addRelationLink(resultBundle, "self", requestPath, page);
        addRelationLink(resultBundle, "first", requestPath, 1);
        int lastPage = (int) Math.ceil((float) resultBundle.getTotal() / count);
        if (page > 1 && page <= lastPage) addRelationLink(resultBundle, "previous", requestPath, page-1);

        if (page < lastPage) addRelationLink(resultBundle, "next", requestPath, page+1);
        addRelationLink(resultBundle, "last", requestPath, lastPage);

        return resultBundle;
    }
}
