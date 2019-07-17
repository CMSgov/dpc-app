package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;

public interface AuthFactory {

    DPCAuthFilter createPathAuthorizer(PathAuthorizer pa);

    DPCAuthFilter createStandardAuthorizer();
}
