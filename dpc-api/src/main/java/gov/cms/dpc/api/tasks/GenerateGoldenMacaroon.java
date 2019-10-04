package gov.cms.dpc.api.tasks;

import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonVersion;
import com.google.common.collect.ImmutableMultimap;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.servlets.tasks.Task;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.util.Collections;

/**
 * Admin task for creating a Golden macaroon which has superuser permissions in the application.
 * This should only ever be called once per environment.
 */
@Singleton
public class GenerateGoldenMacaroon extends Task {

    private final MacaroonBakery bakery;

    @Inject
    GenerateGoldenMacaroon(MacaroonBakery bakery) {
        super("generate-macaroon");
        this.bakery = bakery;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
        final Macaroon macaroon = bakery.createMacaroon(Collections.emptyList());
        output.write(macaroon.serialize(MacaroonVersion.SerializationVersion.V2_JSON));
    }
}
