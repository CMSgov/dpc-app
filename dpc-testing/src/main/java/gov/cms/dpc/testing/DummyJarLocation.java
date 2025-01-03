package gov.cms.dpc.testing;

import io.dropwizard.util.JarLocation;

import java.util.Optional;

public class DummyJarLocation extends JarLocation {
    public DummyJarLocation() {
        super(DummyJarLocation.class);
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.of("1.0.0");
    }
}
