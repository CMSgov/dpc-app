package gov.cms.dpc.aggregation.bbclient;

import org.apache.http.client.methods.HttpGet;
import org.mockito.ArgumentMatcher;

// https://www.baeldung.com/mockito-argument-matchers
public class HttpGetMatcher implements ArgumentMatcher<HttpGet> {

    private HttpGet left;

    public HttpGetMatcher(HttpGet left) {
        this.left = left;
    }

    @Override
    public boolean matches(HttpGet right) {

        if(right == null){
            return false;
        }else {
            return right.getURI().equals(left.getURI());

        }
    }
}
