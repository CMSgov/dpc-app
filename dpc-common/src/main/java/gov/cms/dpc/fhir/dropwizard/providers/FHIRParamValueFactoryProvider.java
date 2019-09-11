package gov.cms.dpc.fhir.dropwizard.providers;

import gov.cms.dpc.fhir.annotations.FHIRParameter;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;
import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
import org.glassfish.jersey.server.model.Parameter;
import org.hl7.fhir.dstu3.model.Group;

import javax.inject.Singleton;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
public class FHIRParamValueFactoryProvider extends AbstractValueFactoryProvider {

    @Singleton
    public static class InjectionResolver extends ParamInjectionResolver<FHIRParameter> {

        public InjectionResolver() {
            super(FHIRParamValueFactoryProvider.class);
        }
    }


    /**
     * Initialize the provider.
     *
     * @param mpep              multivalued map parameter extractor provider.
     * @param locator           HK2 service locator.
     * @param compatibleSources compatible parameter sources.
     */
    protected FHIRParamValueFactoryProvider(MultivaluedParameterExtractorProvider mpep, ServiceLocator locator, Parameter.Source... compatibleSources) {
        super(mpep, locator, compatibleSources);
    }

    @Override
    protected Factory<?> createValueFactory(Parameter parameter) {
        return null;
    }

    private static class FHIRParamValueFactory extends AbstractContainerRequestValueFactory<Group> {

        @Context
        private ResourceContext context;

        private final Parameter parameter;

        FHIRParamValueFactory(Parameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Group provide() {
            return new Group();
        }
    }
}
