# Quick Start Guide

This is a quick start guide for running the DPC API on your local environment. For full documentation, see the [User Guide](./README.md).

SECURE
====

### Create the .vault_password file in the project root:
- Get the secret from team member as stated in [README](./README.md).
- Run the `make secure-envs` command.








### Secrets management




### Sensitive Docker configuration files 

The files committed in the `ops/config/encrypted` directory hold secret information and are encrypted with [Ansible Vault](https://docs.ansible.com/ansible/2.4/vault.html).

In order to encrypt and decrypt configuration variables, you must create a `.vault_password` file in this repository root directory and in the `/dpc-go/dpc-attribution` directory. Contact another team member to gain access to the vault password.

**IMPORTANT:** Files containing sensitive information are enumerated in the `.secrets` file in this directory. If you want to protect the contents of a file using the `ops/scripts/secrets` helper script, it must match a pattern listed in `.secrets`.

To avoid committing and pushing unencrypted secret files, use the included `ops/scripts/pre-commit` Git pre-commit hook from this directory:

```
cp ops/scripts/pre-commit .git/hooks
```

> Note: You can use `make secure-envs` to decrypt files and create the pre-commit hook at the same time.


BUILD
====
Run the `make ci-app` command.



CONFIGURE
====
- Run the `make start-app` command.
- Verify successful launch by running the command `curl -H "Accept: application/fhir+json" http://localhost:3002/v1/metadata`. You should view the server's response if all goes well.

### Edit the `docker-compose.yml` file

#### Ports
When running locally, you'll need to update the docker-compse.yml file by adding:
```yaml
ports: 
  - "5432:5432"
```

in the `db` service. E.g.:
```yaml
db: 
  image: postgres:11 
  ports: 
    - "5432:5432"
```
#### JVM Authentication
Set authentication_disabled for JVM set to **[true]** in the `api` service in the same file.

`JVM_FLAGS=-Ddpc.api.authenticationDisabled=${AUTH_DISABLED:-true}`

By default, the Docker containers start with minimal authentication enabled, meaning that some functionality (such as extracting the organization_id from the access token) will not work as expected and always return the same value.
This can be overridden during startup by setting the `AUTH_DISABLED=false` environment variable.



### Generate a Golden Macaroon
You will need a macaroon for the Docker configuration. Run this command to generate one.
`curl -X POST http://localhost:9903/tasks/generate-token`

You will then need to add the **`API_METADATA URL`** variable and the **`GOLDEN_MACAROON`** variable to the `dpc_web` and `dpc_admin` services in the docker-compose.portal.yml file.
```yaml
dpc-web: 
   
  environment: 
    ... 
    - GOLDEN_MACAROON=newly-generated-macaroon  
    - API_METADATA_URL=http://host.docker.internal:3002/v1
    .. 
  dpc_admin: 
    ...
    - API_METADATA_URL=${API_METADATA URL}
    - GOLDEN_MACAROON=${ ...
```





RUN
====

Run the `make start-dpc`.
- Clear the project's cache by running the command: `docker exec -it ${containerID} rails dev:cache`.
- Request access by visiting http://localhost:3900.
- Simulate the approval process by visiting http://localhost:3900/letter_opener and clicking on the **confirm my account** link.

If no EUA is granted, you will have to manually populate the necessary db tables. Sample tables are provided [here](./DbTables.md).
Once populated, you should see the options to create a public key and a client token.
	
	Create Public Key
		-- Use notepad to convert CRLF to LF
TEST
====
### Test with Postman
Once the development environment is up and running, you should now be able to run some calls to the API via the [DPC Postman Collections](https://dpc.cms.gov/docsV1.html#postman-collection). Below, are some useful endpoints for verifying a functional development environment:
- Register single patient
- Register practitioner
- Get all groups
- Add patient to group
- Create export data request
