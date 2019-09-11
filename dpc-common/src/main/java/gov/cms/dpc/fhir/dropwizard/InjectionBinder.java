package gov.cms.dpc.fhir.dropwizard;

import gov.cms.dpc.fhir.annotations.FHIRParameter;
import gov.cms.dpc.fhir.dropwizard.providers.FHIRParamValueFactoryProvider;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;

import javax.inject.Singleton;

public class InjectionBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bind(FHIRParamValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class);
        bind(FHIRParamValueFactoryProvider.InjectionResolver.class).to(new TypeLiteral<InjectionResolver<FHIRParameter>>() {
        }).in(Singleton.class);
    }
}
