package gov.cms.dpc.common.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

public final class XSSSanitizerUtil {

    private XSSSanitizerUtil() {
        //util class
    }

    public static String sanitize(String unsanitized) {
        String s1 = unsanitized.replaceAll("(\\s&\\s)", "   ");
        return Jsoup.clean(s1, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
    }
}
