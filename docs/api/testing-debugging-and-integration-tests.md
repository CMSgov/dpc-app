## Local Debugging

###### [`^`](#table-of-contents)

If you're running locally through Docker and you want to use your debugger there are two steps.

-   Open up port 5005 on whichever service you want to debug
    -   Add the following to docker-compose.yml under api, aggregation, attribution or consent.
        ```
        ports:
            - "5005:5005"
        ```
-   Instead of using `make start-dpc` or `make start-app` to start the application, use `make start-dpc-debug` or `make start-app-debug`.
    -   They'll both do a clean compile of the app with debug information and start each service with the debug agent.
-   Now you can attach your debugger to the running app on port 5005.
    -   If you're using IntelliJ, there are instructions [here](https://www.jetbrains.com/help/idea/attaching-to-local-process.html#attach-to-remote).

## Debugging Integration Tests

###### [`^`](#table-of-contents)

If you want to run and debug integration tests through IntelliJ there are a few steps you have to do first. The same concepts should apply to VS Code, but you'll have to figure out the details yourself.

-   When running a test in the IDE, IntelliJ creates a temporary debug configuration. We need to make sure our secure env variables get included.
    -   Go to Run -> Edit Configurations
    -   Click Edit Configuration Templates and select JUnit
    -   At the bottom, add a new .env file and point it to `ops/config/decrypted/local.env`
-   We need to start our dependent services, so run `make start-it-debug`
    -   This will recompile dpc with debug extensions included and start containers for dpc-attribution, dpc-aggregation, dpc-consent and a db.
-   Now you should be able to run any of the integration tests under dpc-api by clicking on the little green arrow next to their implementation.
    -   Need to debug a test? Right click on the triangle and select debug.
-   If you have to debug one of the dependant services, for instance because an IT is calling dpc-attribution and getting a 500, and you can't figure out why, follow the instructions under [Local Debugging](#local-debugging) to open up the dependant service's debugger port in docker-compose, then rerun `make start-it-debug`.
    -   Now you can attach your debugger to that service and still run integration tests as described above.
    -   You'll have one debugger tab open on an IT in dpc-api and another on the dependant service, allowing you to set break points in either and examine the test end to end.

#### Running Integration Tests Against the BFD Sandbox

Want to run your integration tests against the real BFD sandbox instead of using the MockBlueButtonClient? In docker-compose.yml, under the aggregation service, set the USE_BFD_MOCK env variable to true and then rerun `make start-it-debug.`

Note: Many of our integration tests are written for specific test data that only exists in our MockBlueButtonClient. If you switch to the real BFD sandbox these tests will fail, but if you want a true end to end test this is the way to go. A list of synthetic patients in the sandbox can be found [here](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide).
