# Quick Start Guide

This is a quick start guide for running the DPC API on your local environment. For full documentation, see the [User Guide](./README.md).

Be sure to pre-install all necessary software (JDK, Docker, etc.) as described in the full documentation.

SECURE
====

### Create the .vault_password file in the project root:

The files committed in the `ops/config/encrypted` directory hold secret information and are encrypted with [Ansible Vault](https://docs.ansible.com/ansible/2.4/vault.html).

In order to encrypt and decrypt configuration variables, you must create a `.vault_password` file in the root directory. Contact another team member to gain access to the vault password.

Afterwards, run the following to decrypt files for local development:

```sh
make secure-envs
```

This will decrypt the secrets contained in the ops/config/encrypted/local.env file and write a decrypted version to ops/config/decrypted/local.env.
Both the .vault_password and ops/config/decrypted/local.env files are included in this project's .gitignore and should never be committed and pushed to the project's public git repository.

BUILD
====

The following steps will obtain the project, build the docker images to host the system, compile and package the software and install it on the docker images.

1) Clone the dpc-app project from https://github.com/CMSgov/dpc-app (main branch and desired dev branches). Use `git clone` or your IDE.

2) From the project directory, run the command `make docker-base website admin portal` to pull and build the docker images used by the project.

3) From the project directory, run the command `JAVA_HOME=<jdk11 path> make api` to build and package the software.

RUN
====

The following commands will launch docker containers that start the API services.

1) From the project directory, run the command, `make start-app`, to launch containers running the API and its dependent services`.

2) From the project directory, run the command, `make start-portals`, to launch containers running the websites for administering access to the API.

USE
====

To prime a new system with an empty database, 

1) Visit the website at http://localhost:3900 to see the website where access by a client organization may be requested. The step of requesting access is not necessary in development unless you want to explore these features.

2) Simulate the approval process by visiting http://localhost:3900/letter_opener and clicking on the **confirm my account** link.

3) From the project directory, run the command, `make portal-sh`, to launch a command line for creating the invitation link needed for a client administrator to gain access to the administration site.

4) Enter the command, `rails dpc:invite_ao INVITE=<firstName>,<lastName>,<emailAddress>,<orgID e.g. 7838426501>`. This command will insert the user info into the database and generate an invitation URL.

5) Visit the URL returned in the previous step. You will be asked to sign in to the SANDBOX version of login.gov, a unified Federal service for performing online identity-proofing and permitting authentication to supporting web sites and applications. The sandbox version is for testing and does not require real identity information nor does it verify the information submitted for establishing a test account.  The email address used in registering with the sandbox login.gov does need to be real and accessible. After creating a sandbox login.gov account and signing into it, you will be returned to the DPC portal. 

6) In order to use the API, you will require:
        a) to obtain a client token, which is generated and provided by the portal;
        b) to generate a key pair for signing JSON web tokens and submit the public key and evidence of its validity to the portal;
        c) the public key ID assigned by the portal to the key submitted in step (6b).


TEST
====

There are four stages of test that can be done locally with the dpc-app.

1) Unit tests of the Java software projects can be run locally from the project directory with the command, `JAVA_HOME=<jdk11 path> make unit-tests`. This will run tests, but not compile changes.  To compile changes and test them, run the command, `JAVA_HOME=<jdk11 path> mvn install`.

2) Integration tests of the Java software projects can be run from the project directory with the command, `make int-tests`. This will launch docker containers to host the services and a tests container to run the integration tests.

3) System tests of the Java software projects can be run from the project directory with the comamnd, `make sys-tests`. This will launch the full set of API services in containers and run tests using the project's Postman collection.

Note: unit, integration,and system test can be run back-to-back by running the command, `make ci-app`, from the project directory.

4) Open testing of the API via Postman can be done by downloading the project's Postman collection (linked in full documentation) and configuration the Postman environment with the client token, private signing key for JWT signing, and public key key-id as discussed above.

5) Smoke tests can be run on the system to verify its online integration and performance. To run smoke tests, from the project directory, run the command, `make start-smoke-local`, to launch the necessary containers and configuration. Once the containers are healthy, run the command, `JAVA_HOME=<jdk11 path> make smoke/local`. 