# CLI Commands
Dropwizard offers the ability to add CLI commands to a project.  Currently DPC has three projects with custom CLI commands; Attribution, API, and Consent. In addition to the custom CLI commands, Dropwizard has built-in default commands for `server` (starts up app), `check` (validates configuration), `db` (various db tasks).

## API CLI Commands
API has three custom commands: `organization`, `key`, and `token`.  Each of these commands has additional subcommands.

#### Organization Command
The host URL (`--host`) for the command requires the Attribution host URL.  The three operations you can do on an organization are `delete`, `list`, and `register`.

#### Description
* `delete` - requires the organization ID and prints out a success message
* `list` - prints out a table with the columns, organization id, npi, and name
* `register` - requires an organization file and optional parameters are creating/not creating a token and the api host url. It will print out the organization id and token if set to create

#### Examples
* `java -jar dpc-api/target/dpc-api.jar --host ${ATTRIBUTION_HOST} organization delete ${org_id}`
* `java -jar dpc-api/target/dpc-api.jar --host ${ATTRIBUTION_HOST} organization list`
* `java -jar dpc-api/target/dpc-api.jar --host ${ATTRIBUTION_HOST} organization register --file ${org_file} --no-token ${true|false} --api ${API_HOST}`

---
#### Token Command
The host url (--host) for the command requires the api tasks endpoint. The three operations you can do on a token is `create`, `delete`, and `list`.

#### Description
* `create` - requires the organization id and optionally the label and expiration date (ISO_OFFSET_DATE_TIME format). It prints out the token when successful
* `delete` - requires the organization id and token id. It prints out a success message
* `list` - requires the organization id and prints table with the columns `token id`, `label`, `type`, `created at`, `expires at`

#### Examples
* `java -jar dpc-api/target/dpc-api.jar --host ${API_TASKS_URL} token create ${org_id} --label ${label} --expiration ${expiration_date}`
* `java -jar dpc-api/target/dpc-api.jar --host ${API_TASKS_URL} token delete --org ${org_id} ${token_id}`
* `java -jar dpc-api/target/dpc-api.jar --host ${API_TASKS_URL} token list ${org_id}`

---
#### Key Command
The host url (--host) for the command requires the api tasks endpoint. The three operations you can do on a token is `upload`, `delete`, and `list`.

#### Description
* `delete` - requires the organization ID and the public key ID. Prints a success message
* `list` - requires the organization id. It prints out a table with the columns `key id`, `label`, `created at`
* `upload` - requires the organization id and public key file and optionally the label for the public key. It prints out the organization token

#### Example
* `java -jar dpc-api/target/dpc-api.jar --host ${API_TASKS_URL} key delete --org ${org_id} ${public_key_id}`
* `java -jar dpc-api/target/dpc-api.jar --host ${API_TASKS_URL} key list ${org_id}`
* `java -jar dpc-api/target/dpc-api.jar --host ${API_TASKS_URL} key upload ${org_id} --label ${label} ${file_path}`

## Consent CLI Commands
Consent has two custom commands: `seed` and `consent`. The `consent` command has additional subcommands

#### Seed Command
Seeds the consent database with data from the `test_consent.csv` file

#### Example
* `java -jar dpc-consent/target/dpc-consent.jar seed`

---
#### Consent Command
The host url (--host) for the command requires the attribution url. The only current operation available is creating a consent.

#### Description
* `create` - Adds a consent record for a specific beneficiary to the consent database. While most consent records are recorded through our consent ETL process, we want to be ready to create single consent records on demand where the custodian can be an external organization or DPC.

#### Example
* `java -jar dpc-consent/target/dpc-consent.jar consent create --host ${ATTRIBUTION_HOST} --patient ${MBI} --in/--out --date ${effective_date} --org ${org_id}`

## Attribution CLI Commands
Attribution has one custom command: `seed`

#### Seed Command
Seeds teh attribution database with data from the `test_associations.csv` file

#### Description
* `seed` - has an optional timestamp parameter which is used when adding attributed relationships.

#### Example
* `java -jar dpc-attribution/target/dpc-attribution.jar seed --timestamp ${timestamp}`

# Tasks
A Task is a run-time action your application provides access to on the administrative port via HTTP
To learn more about tasks, click [here](https://www.dropwizard.io/en/latest/manual/core.html#tasks)

### API Tasks
In addition to the `DeleteToken`, `GenerateClientTokens`, `ListClientTokens`, `DeletePublicKey`, `ListPublicKeys`, and
 `UploadPublicKey` used by the above CLI API Commands, there is also a `GenerateKeyPair` task.

The `GenerateKeyPair` task is an Admin task to create a new BakeryKeyPair for use by the 
gov.cms.dpc.macaroons.MacaroonBakery component. This will generated an X25519 keypair that is used to encrypt the 
third-party caveats.

### Attribution Tasks
The `TruncateDatabase` task is an Admin task is for truncating tables in the attribution database.
