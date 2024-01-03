package gov.cms.dpc.testing;

import gov.cms.dpc.testing.exceptions.SystemExitException;

import java.security.Permission;

/**
 * Use this to test classes that call System.exit() to prevent them from exiting early.
 * Before your test, call...
 * System.setSecurityManager(new NoExitSecurityManager())
 * and after make sure to reset it.  Instead of exiting when the class being tested calls System.exit(), a
 * RuntimeException will be thrown with its message set to the exit status.
 */
public class NoExitSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm) {
        // Do nothing
    }

    @Override
    public void checkExit(int status) {
        super.checkExit(status);
        throw new SystemExitException(String.valueOf(status));
    }
}
