# Quick Start Guide

This is a quick start guide for running the DPC API on your local environment. For full documentation, see the [User Guide](./README.md).

To investigate project lifecycle controls, review the Makefile, docker-compose files, and .sh shell scripts in the main project directory to understand more about the software and commands that control the project.

PROJECT QUICKSTART
====

### Full Project Build and Test

To compile, package, and test the software with a single command, run the following make target from the project directory (be sure your JAVA_HOME or default java executable is JDK 11):

```
make docker-base admin website portal ci-app
```

PROJECT LIFECYCLE
====
The developer lifecycle consists of these phases:

1) Clone
2) Configure
3) Develop
4) Compile and Package
5) Test
6) Debug
7) Commit
8) Push

CLONE
====

### Clone the dpc-app project from github to begin development

The dpc-app github repo is publically accessible and may be cloned without any github credentials. The `git` command line tool or an IDE with integrated git support may be used. In addition to the `main` branch of the project, additional branches may be cloned to analyze or continue work-in-progress.

To clone the main branch of dpc-app only, an example command line statement would be:

```
git clone https://github.com/CMSgov/dpc-app.git
```


CONFIGURE
====

### Configure JDK and security tokens for project packaging and integration

This project currently supports JDK 11. Your IDE and terminal environments should all be configured to use JDK 11 when running any commands that involve java or maven.

The files committed in the `ops/config/encrypted` directory hold secret information and are encrypted with [Ansible Vault](https://docs.ansible.com/ansible/2.4/vault.html).

In order to encrypt and decrypt configuration variables, you must create a `.vault_password` file in the root directory. Contact another team member to gain access to the vault password.

Afterwards, run the following to decrypt files for local development:

```
make secure-envs
```
This command created a decrypted version of the configuration variables in ops/config/decrypted. This directory is included in the `.gitignore` file for this project and should never be committed to the project repo.  Additionally, the values in this file may appear in local debug or trace logging, which should be routinely deleted before committing.  Java Maven also requires these tokens. To produce the directory and file format for maven, run the command:

```
make maven-config
```

which produces a `.mvn` directory in the project folder with maven-formatting for the same secure token values. Similarly, this directory is included in the `.gitignore` for this project.

This project makes use of several docker images to host software locally and in the online CI/CD environment for test. Docker builds must be prepared to support all subsequent commands. To build the necessary docker images, run the following make targets:

```
make docker-base website admin portal
```

DEVELOP
====

### Preferred IDE: IntelliJ

While this project should be supported by any contemporary Java IDE, the preferred IDE is IntelliJ. Be sure to confirm that your selected IDE debugging functions (in-IDE and remote) work correctly as you progress in development. Additionally, check that any IDE-generated files (e.g. configuration files) are not included in your commits to the project as we maintain an IDE-agnostic development process. If you IDE includes maven support, you should be able to configure maven actions consistent with those found in the project Makefile and various .sh scripts in order to perform in-IDE build and test operations as desired.

In addition to IntelliJ, this project has been verified to work with Apache NetBeans and Microsoft Visual Studio. 

COMPILE AND PACKAGE
====

### Load Software to Docker

When you are ready to test or run your software outside the IDE, run the following make target:

```
make api
```


This command operates on the project and each of its modules. It cleans each target directory, compiles the java software, and loads the operational software (the api and the aggregation, attribution, and consent services) to their respective docker container images. 

Your IDE's integrated clean-and-build capability should be able to do this, as well, once configured consistent with the Makefile target above. For the API, consent, attribution, and aggregation modules, if your IDE reports build errors after compiling, try disabling the plugin that loads the compiled software to docker by adding the `jib.skip=true` setting to your maven action configuration.

TEST
====

### Five Progressive Local Test Stages

This project includes five local test capabilities before pushing software to github for online CI/CD testing. Tests should be performed only after re-building the project or specific modules that have been updated per the `Compile and Package` section.

* Unit Test
    - Unit tests are used to verify individual classes and methods perform their function correctly, in isolation of any external dependencies (which are generally mocked via the Mockito framework)
    - To unit test the entire software project, run the following make target:
```
        mvn install
```

* Integration Test
    - Integration tests are used to verify that software components successfully interact to perform more complex functions. They involve the software components being loaded to docker images and containers being started to provide the online support to execute tests.
    - To perform integration tests for the entire software project, run the following make target:
```
        make int-tests
```

* End-to-End System Test
    - System tests are used to verify that the backend software (API and supporting services) can be utilized by the front-end software (a web portal for credential administration) to grant user access.
    - To perform system tests for the entire software project, run the following make target:
```
        make sys-tests
```

* Smoke Test
    - Smoke tests are used to verify that the complete deployed and operational software appears to perform satisfactorily and without error while supporting relatively simple utilization.
    - To perform smoke tests for the entire software project, run the following make target and, when prompted to `press enter` once the system is running, do so (as the `start-system-smoke` target has already run and brought the system online):
```
        make start-system-smoke smoke/local
```

* Live Test
    - Live tests are used so a user/client can interact with the front-end and back-end system components as desired to produce valid and error conditions, understand performance, etc.
    - To perform live tests for the entire software project, 
        * Run the following make target: `make start-app start-portals`. This command will start all dpc-app services and permit a user to access the web portal via `http://localhost:3900` to begin the portal process. 
        * To issue a user invitation without the initial portal process, run the make target: `make portal-sh` and in the portal shell, run the command `rails dpc:invite_ao INVITE=<firstName>,<lastName>,<emailAddress>,<identifier>`
        where the email address matches a login.gov sandbox account. Utilize the returned invitation URL to establish a portal account and begin issuing credentials for system use.
        * Download the [DPC Postman Collections](https://dpc.cms.gov/docsV1.html#postman-collection) and follow the included setup instructions to begin interacting with the API through client software.
          - Note that when using Postman, some data from the collection may be stored in your Postman cloud account to support roaming. Be sure to configure your use of this Postman collection to protect sensitive data (e.g., cryptographic keys) to only be stored locally.
DEBUG
====

### Debugging of dpc-app software components

To debug software, there are three suggested approaches:

1) Debug unit tests directly in your IDE.  Your IDE should permit executing a selected unit test or test file in debug mode and setting desired debug features (e.g. breakpoints, watches, etc) in the associated class/method being tested.

2) Debug integration tests directly in your IDE.  To debug an integration test, your IDE should permit executing a selected integration test or test file in debug mode, just as with unit testing. However, in this case, any supporting software (database, consent/attribution/aggregation) needed to facilitate the test (but which itself isn't being debugged) should be launched before testing. To launch the database, run the make target, `make start-db`. To launch a selected support service, run the make target, `make start-<service>` (i.e., start-consent, start-attribution, start-aggregation).

3) Compile the software with debug symbols and attach a remote debugger from your IDE.  Run the make target, `make start-app-debug`, which will recompile the software and start the API and support services with debug ports exposed. To confirm or change the debug ports for each service, consult the `docker-compose.debug-override.yml` file.

COMMIT
====

### Commiting verified changes to the project

The dpc-app project does not permit direct changes to the main branch. Create a new branch anytime you are developing a new feature, project enhancement, bug fix, or other change. As your development proceeds, you can incrementally commit changes into logical groups of advancement, or perform a single commit for all work on the branch. Since reviewers are left to understand the work done in the branch, take into consideration the best way to use commits to make that review manageable. 

Additionally, since commits to your branch can be backed off using `git reset --hard` and `git push --force`, consider eliminating any exploratory or experimental commits once a thread of work is complete in order to clean up the git history and make the work clearer for review.

PUSH
====

### Submit your branch work

As your branch work progresses and at its completion, push local commits to the project's github repo via the command line, `git push`, or through your IDE's integrated GIT tools.

When your branch work is ready for online CI/CD testing and peer review, open a Pull Request (PR) in github for your branch. The same tests that run locally will be executed in AWS and, additionally, code quality scans are run to report on the state of the software.

CLEANUP
====

### Resetting the local environment during development

There are two main aspects of cleanup that can be performed during development.

1) To clean existing compiled software, run the commnd, `mvn clean` from your project directory or a specific module directory. This removes an existing target and its dependencies. You will be required to re-compile the software to continue working with it thereafter.
2) Run the make target, `docker ps`, to confirm if any docker containers are running. Docker containers are grouped by project name.  The three make targets, `make down-dpc`, `make down-portals`, and `make down-start-v1-portals` are available for convenience. Their shell commands can be reviewed so you can bring down other docker containers by command line. You can also control container status graphically in Docker Desktop, if available.