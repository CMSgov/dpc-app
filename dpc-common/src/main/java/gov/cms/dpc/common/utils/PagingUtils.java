package gov.cms.dpc.common.utils;

import ca.uhn.fhir.rest.gclient.IQuery;
import org.hl7.fhir.dstu3.model.Bundle;

public class PagingUtils {
    private PagingUtils() {
        throw new UnsupportedOperationException("PagingUtils is a utility class and should not be instantiated");
    }

    public static final int DEFAULT_LIMIT = 1;

    private static String formatURL(String url, int page) {
        return url + "?page=" + page;
    }

    private static void addRelationLink(Bundle bundle, String name, String path, int page) {
        bundle.addLink().setRelation(name).setUrl(formatURL(path, page));
    }

    public static Bundle handlePaging(IQuery<Bundle> request, int count, int page, String requestPath) {
        request.offset(count*(page-1));
        request.count(count);
        Bundle resultBundle = request.execute();

        addRelationLink(resultBundle, "self", requestPath, page);
        addRelationLink(resultBundle, "first", requestPath, 1);
        if (page > 1) addRelationLink(resultBundle, "previous", requestPath, page-1);

        int lastPage = (int) Math.ceil((float) resultBundle.getTotal() / count);
        if (page + 1 <= lastPage) addRelationLink(resultBundle, "next", requestPath, page+1);
        addRelationLink(resultBundle, "last", requestPath, lastPage);

        return resultBundle;
    }
}
