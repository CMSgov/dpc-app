package gov.cms.dpc.aggregation;

import gov.cms.dpc.bluebutton.client.BlueButtonClient;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/test")
public class TestResource {

    @Inject
    public TestResource(BlueButtonClient client) {
        client.requestCapabilityStatement();
    }


    @GET
    public String getIt() {
        return "Hello";
    }
}
