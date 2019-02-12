package gov.cms.dpc.web;

import ca.mestevens.java.configuration.TypesafeConfiguration;

public class DPWebConfiguration extends TypesafeConfiguration {

    private String testValue;

    DPWebConfiguration() {
//        Not used;
    }

    public String getTestValue() {
        return this.testValue;
    }

    public void setTestValue(String testValue) {
        this.testValue = testValue;
    }


    // TODO: implement service configuration
}
