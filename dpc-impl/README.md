# DPC Implementer Portal
This is the IN-PROGRESS web-application for Data at the Point of Care (DPC) Implementers. As of 10/21/2022, the application has NOT been released to production and is not considered production ready. 

## Specs
* Ruby
* Postgresql
* Docker

This README assumes you've installed all of the above specs. For more information about how to install them on your machine, see the main repository [README](https://github.com/CMSgov/dpc-app/blob/master/README.md).

## Configuration Notes

The dpc-impl web-application communicates with the [DPC V2 API](https://github.com/CMSgov/dpc-app/tree/master/dpc-go/dpc-api) and the [V2 Attribution service](https://github.com/CMSgov/dpc-app/tree/master/dpc-go/dpc-attribution). Both V2 of the API and V2 of the attribution service are written in Golang and are required to run the Implementer web-app.

The dpc-impl service uses the [BCDA System-to-System Authentication Service (SSAS)](https://github.com/CMSgov/bcda-ssas-app) to exchange secure credentials with the api service. The BCDA SSAS service is written in Golang and is required to run the Implemeter web-app.

## Running the App Locally

### Decrypting secrets 

The V2 attribution service requires a ca cert file, a public cert file and a key file. The encrypted files can be found [here](https://github.com/CMSgov/dpc-app/tree/master/dpc-go/dpc-attribution/shared_files/encrypted). Run the command below to decrypt the files and make them available to the attribution service. 

```Bash
make bfd-certs
```

### Turn off MTLS in api-go and dpc-attribution

Both V2 versions of the api service and the attribution service have Mutual TLS turned on by default. For local development, this can be turned off. 

1. In the `dpc-go/dpc-attribution/src/main.go` file on line 71, change authType from "TLS" to "NONE"
3. In the `dpc-go/dpc-api/src/service/admin/router.go` file on line 99, change authType from "TLS" to "NONE"

### Update Docker urls 

1. In the `docker-compose.portals.yml` file, set `API_METADATA_URL` to `http://host.docker.internal:3011/api/v2`
3. In the `dpc-go/dpc-api/docker-compose.yml` file
    *  Set `DPC_ATTRIBUTION-CLIENT_URL` to `http://host.docker.internal:3001` 
    * Set `DPC_SSAS-CLIENT_ADMIN-URL` to `http://host.docker.internal:3104`
    * Set `DPC_SSAS-CLIENT_PUBLIC-URL` to `http://host.docker.internal:3103`

### Build the V2 Docker images and spin up the containers

This `make` command will build all of the necessary Docker images required to run V2 on your local machine

```Bash
make build-v2
```

This `make` command will spin up all of the necessary Docker containers required to run V2, including the dpc-impl service,the V2 api service, V2 attribution service, the consent service, the aggregation service and a postgres database.

```Bash
make start-v2
```

### Load SSAS fixtures

SSAS requires initialization in order for it to act as the go-between between the various services. This make command combines several of the SSAS CLI commands listed [here](https://github.com/CMSgov/bcda-ssas-app#bootstrapping-cli) in order to build the database, seed the database, and create an admin system.

```Bash
make load-ssas-fixtures
```

### Generate a new SSAS client secret

Now that the SSAS database is seeded with the admin system, we need a new client secret to feed to the V2 api.

1. Run the following `make` command in your console and copy the outputted secret value
```Bash
make ssas-creds
```
2. In the `dpc-go/dpc-api/docker-compose.yml` file, set `DPC_SSAS-CLIENT_CLIENT-SECRET` to the output of the above command

### Restart V2 API

Since we've updated the V2 api docker-compose.yml file, we need to restart the V2 api service

```Bash
docker-compose -p dpc-v2 -f dpc-go/dpc-api/docker-compose.yml up -d --build api
```

### Sign into the DPC Implementer Portal

Log into the portal at http://localhost:4000/impl

After requesting access, you can access the email inbox at http://localhost:4000/impl/letter_opener

## Application Notes

* When testing uploading public keys and signature files in the DPC Implementer Portal, note that the snippet.txt file that SSAS will accept is NOT the same snippet.txt file that the DPC V1 API will accept. Download the snippet.txt file directly from the DPC Implementer Portal or from [here](https://github.com/CMSgov/dpc-app/tree/master/dpc-impl/public/downloads).