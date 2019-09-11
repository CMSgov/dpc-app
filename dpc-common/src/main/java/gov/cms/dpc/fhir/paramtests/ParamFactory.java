package gov.cms.dpc.fhir.paramtests;

import ca.uhn.fhir.context.FhirContext;
import com.google.inject.Injector;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Parameters;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;

public class ParamFactory implements Factory<Group> {

//    @Context
//    private HttpServletRequest request;
//    @Context
//    @RequestParameters
//    private Map<String, String[]> params;

    private final Injector injector;
    private final FhirContext ctx;
    private final Parameter parameter;

    ParamFactory(Injector injector, Parameter parameter, FhirContext ctx) {
        this.injector = injector;
        this.ctx = ctx;
        this.parameter = parameter;
    }

    @Override
    public Group provide() {
        final HttpServletRequest request = injector.getInstance(HttpServletRequest.class);
        final Parameters parameters;
        try {
            parameters = ctx.newJsonParser().parseResource(Parameters.class, request.getInputStream());
        } catch (IOException e) {
            throw new WebApplicationException("Cannot parse input stream", e);
        }
        // Get the first parameter
        return (Group) parameters.getParameterFirstRep().getResource();
    }

    @Override
    public void dispose(Group instance) {

    }
}
