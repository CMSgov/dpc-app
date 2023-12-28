package gov.cms.dpc.api;

import java.security.Permission;

/**
 * Use this to test classes that call System.exit() to prevent them from exiting early.
 * Before your test, call...
 * System.setSecurityManager(new NoExitSecurityManager())
 * and after make sure to reset it.
 * Instead of exiting when the class being tested calls System.exit(), an exception will be thrown.
 */
class NoExitSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm) {
        // Do nothing
    }

    @Override
    public void checkExit(int status) {
        // Do nothing
    }
}
