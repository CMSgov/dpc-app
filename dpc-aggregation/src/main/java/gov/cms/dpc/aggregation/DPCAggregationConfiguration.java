package gov.cms.dpc.aggregation;

import ca.mestevens.java.configuration.TypesafeConfiguration;
import org.hibernate.validator.constraints.NotEmpty;

public class DPCAggregationConfiguration extends TypesafeConfiguration {

    @NotEmpty
    private String exportPath;

    public String getExportPath() {
        return exportPath;
    }
}
