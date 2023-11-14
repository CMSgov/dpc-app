# Code Coverage

-   Run `make unit-tests` to use Jacoco to generate local code coverage reports. Within each module, the human-readable report can be found at `[module]/target/site/jacoco/index.html`. The machine-readable version that gets loaded to SonarQube is `jacoco.xml` in the same directory.
-   Stand up a local version of SonarQube inside a Docker container as described [here](https://docs.sonarsource.com/sonarqube/latest/try-out-sonarqube/). Essentially, just run the following command `docker run -d --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 sonarqube:latest`.
    -   Login to SonarQube at http://localhost:9000 with login:pass of admin:admin.
    -   Setup a new project as described in the link above.
-   Run the following command to load your coverage data into SonarQube, inserting your project key, name and token...
    ```
    mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar \
      -Dsonar.projectKey={YOUR PROJECT KEY} \
      -Dsonar.projectName='{YOUR PROJECT NAME}' \
      -Dsonar.host.url=http://localhost:9000 \
      -Dsonar.token={YOUR PROJECT TOKEN}
    ```
-   Your code coverage results should now be in your local version of SonarQube.

TODO: You can also run the individual makefile commands for building applications (make ci-app)
