# CLI Commands
Dropwizard offers the ability to add CLI commands to a project.  Currently DPC has three projects with custom CLI 
commands; Attribution, API, and Consent. In addition to the custom CLI commands, Dropwizard has built-in default commands 
for `server` (starts up app), `check` (validates configuration), `db` (various db tasks). You can get more information on
the commands by calling help with the `-h` flag after any command or subcommands. At the very least, the help will give 
you an idea of what parameters are needed for the CLI command.

## API CLI Commands
API has three custom commands: `organization`, `key`, and `token`.  Each of these commands has additional subcommands.

#### Organization Command
The host URL (`--host`) for the command requires the Attribution host URL.  The three operations you can do on an organization are `delete`, `list`, and `register`.

#### Description
* `delete` - requires the organization ID and prints out a success message
* `list` - prints out a table with columns: organization ID, NPI, and name
* `register` - requires an organization file; optional parameters are creating/not creating a token and the API host URL. It will print out the organization ID and token if set to create

#### Examples
* `java -jar dpc-api/target/dpc-api.jar organization delete --host ${ATTRIBUTION_HOST}  ${org_id}`
* `java -jar dpc-api/target/dpc-api.jar organization list --host ${ATTRIBUTION_HOST}`
* `java -jar dpc-api/target/dpc-api.jar organization register --host ${ATTRIBUTION_HOST} --file ${org_file} --no-token ${true|false} --api ${API_HOST}`

---
#### Token Command
The host URL (--host) for the command requires the API tasks endpoint. The three operations you can do on a token are `create`, `delete`, and `list`.

#### Description
* `create` - requires the organization ID; a label and expiration date (ISO_OFFSET_DATE_TIME format) are optional. It prints out the token when successful
* `delete` - requires the organization ID and token ID. It prints out a success message
* `list` - requires the organization ID and prints table with the columns `token id`, `label`, `type`, `created at`, `expires at`

#### Examples
* `java -jar dpc-api/target/dpc-api.jar token create --host ${API_TASKS_URL} ${org_id} --label ${label} --expiration ${expiration_date}`
* `java -jar dpc-api/target/dpc-api.jar token delete --host ${API_TASKS_URL} --org ${org_id} ${token_id}`
* `java -jar dpc-api/target/dpc-api.jar token list --host ${API_TASKS_URL} ${org_id}`

---
#### Key Command
The host URL (--host) for the command requires the API tasks endpoint. The three operations you can do on a token are `upload`, `delete`, and `list`.

#### Description
* `delete` - requires the organization ID and the public key ID. Prints a success message
* `list` - requires the organization ID. It prints out a table with the columns `key id`, `label`, `created at`
* `upload` - requires the organization ID and public key file; a label for the public key is optional. It prints out the organization token

#### Example
* `java -jar dpc-api/target/dpc-api.jar key delete --host ${API_TASKS_URL} --org ${org_id} ${public_key_id}`
* `java -jar dpc-api/target/dpc-api.jar key list --host ${API_TASKS_URL} ${org_id}`
* `java -jar dpc-api/target/dpc-api.jar key upload --host ${API_TASKS_URL} ${org_id} --label ${label} ${file_path}`

## Consent CLI Commands
Consent has two custom commands: `seed` and `consent`. The `consent` command has additional subcommands

#### Seed Command
Seeds the consent database with data from the `test_consent.csv` file

#### Example
* `java -jar dpc-consent/target/dpc-consent.jar seed`

---
#### Consent Command
The host URL (--host) for the command requires the attribution URL. The only current operation available is creating a consent.

#### Description
* `create` - Adds a consent record for a specific beneficiary to the consent database. While most consent records are recorded through our consent ETL process, we want to be ready to create single consent records on demand where the custodian can be an external organization or DPC.

#### Example
* `java -jar dpc-consent/target/dpc-consent.jar consent create --host ${ATTRIBUTION_HOST} --patient ${MBI} --in/--out --date ${effective_date} --org ${org_id}`

## Attribution CLI Commands
Attribution has one custom command: `seed`

#### Seed Command
Seeds the attribution database with data from the `test_associations.csv` file

#### Description
* `seed` - has an optional timestamp parameter which is used when adding attributed relationships.

#### Example
* `java -jar dpc-attribution/target/dpc-attribution.jar seed --timestamp ${timestamp}`

# Tasks
A Task is a run-time action your application provides access to on the administrative port via HTTP
To learn more about tasks, click [here](https://www.dropwizard.io/en/latest/manual/core.html#tasks)

### API Tasks
In addition to the `DeleteToken`, `GenerateClientTokens`, `ListClientTokens`, `DeletePublicKey`, `ListPublicKeys`, and
 `UploadPublicKey` used by the above CLI API Commands, there is also a `GenerateKeyPair` task. All the tasks that related
 to keys call the key resource and all the tasks that relate to tokens call to token resource.

The `GenerateKeyPair` task is an Admin task to create a new BakeryKeyPair for use by the 
gov.cms.dpc.macaroons.MacaroonBakery component. This will generated an X25519 keypair that is used to encrypt the 
third-party caveats.

The `GenerateClientTokens` task is an Admin task that is used by the `key upload` CLI command but in addition to providing 
functionality for the CLI command, the task can be executed directly without passing the `organization` parameter to
generate a golden macaroon. e.g. `curl -X POST http://localhost:8081/tasks/generate-token`

### Attribution Tasks
The `TruncateDatabase` task is an Admin task for truncating tables in the attribution database.
