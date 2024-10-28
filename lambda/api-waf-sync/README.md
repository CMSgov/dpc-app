# Run tests locally

-   Get short-term AWS credentials through CloudTamer. If not available in your quick access dashboard, you can find these credentials in CloudTamer by selecting your project, navigating to Cloud Management > Cloud Access Roles, choosing the relevant application role, selecting the IAM role, and clicking on "Short-Term Access Keys".
-   `make test` for testing in the TEST env.
-   Note: the test updates the live IP address set `dpc-test-api-customers`. If it fails midway (unlikely), it's possible that the IP set has been altered and not reset to the original set, so watch for that.
