package gov.cms.dpc.common.utils;

import ca.uhn.fhir.rest.gclient.IQuery;
import org.hl7.fhir.dstu3.model.Bundle;

public class PagingUtils {
    public static final int defaultLimit = 1;

    private static String formatURL(String url, int page) {
        return url + "?page=" + page;
    }

    private static void addRelationLink(Bundle bundle, String name, String path, int page) {
        bundle.addLink().setRelation(name).setUrl(formatURL(path, page));
    }

    public static Bundle handlePaging(IQuery<Bundle> request, int limit, int page, String requestPath) {
        Bundle resultBundle = request.offset(limit*(page-1)).count(limit).execute();

        addRelationLink(resultBundle, "self", requestPath, page);
        addRelationLink(resultBundle, "first", requestPath, 1);
        if (page > 1) addRelationLink(resultBundle, "previous", requestPath, page-1);

        int lastPage = (int) Math.ceil((float) resultBundle.getTotal() / limit);
        if (page + 1 <= lastPage) addRelationLink(resultBundle, "next", requestPath, page+1);
        addRelationLink(resultBundle, "last", requestPath, lastPage);

        return resultBundle;
    }
}
