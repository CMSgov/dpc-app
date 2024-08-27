package gov.cms.dpc.common.utils;

import ca.uhn.fhir.rest.gclient.IQuery;
import org.hl7.fhir.dstu3.model.Bundle;

public class PagingUtils {
    public static final int defaultLimit = 1;

    private static String formatURL(String url, int page) {
        return url + "?page=" + page;
    }
    public static Bundle handlePaging(IQuery<Bundle> request, int limit, int page, String requestPath) {
        int offset = limit*(page-1);
        request.offset(offset);
        request.count(limit);
        Bundle resultBundle = request.execute();

        resultBundle.addLink().setRelation("self").setUrl(formatURL(requestPath, page));
        resultBundle.addLink().setRelation("first").setUrl(formatURL(requestPath, 1));
        if (page > 1) {
            resultBundle.addLink().setRelation("previous").setUrl(formatURL(requestPath, page-1));
        }

        int total = resultBundle.getTotal();
        int lastPage = total == 0 ? 1 : (int) Math.ceil((float) total / limit);
        if (page + 1 <= lastPage) {
            resultBundle.addLink().setRelation("next").setUrl(formatURL(requestPath, page+1));
        }
        resultBundle.addLink().setRelation("last").setUrl(formatURL(requestPath, lastPage));

        return resultBundle;
    }
}
