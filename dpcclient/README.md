# DPC Client

dpcclient is an executable Go command line program that helps you explore the Data at the Point of Care API before writing your own code. It follows the guidelines described in the [DPC Documentation](https://dpc.cms.gov/docs). 

This project contains executable files for Macs and Windows 10 machines. If you have go 1.11 or better installed, you can also build the code yourself with the following commands. *Note*: In Go 1.11 and 1.12, first ensure that the environment variable `GO111MODULE` is set to `on`. You can do this by running `go env -w GO111MODULE=on`.

```
go get -u github.com/gbrlsnchs/jwt/v3
go build
```

To build a Windows executable:

```
env GOOS=windows GOARCH=amd64 go build 
```

The remainder of this document focuses on using the tool.

# Using DPC Client

While describing dpcclient commands below, we will refer to sections of that documentation. The code itself also cites the documentation it is following.

For convenience, the steps required for engaging with the open sandbox are as follows:

1. Complete [this form](https://dpc.cms.gov/users/sign_up) to request access. After you receive a reply email welcoming you to DPC, continue with the following steps.
1. Log in to the web UI
1. You should see two things you can do: generate client tokens and upload public keys. If you do not see these options, an admin needs to complete setup on your account.
1. Generate a client token
   1. Copy and paste the screen display of the token into a file to save it
   1. You will not be able to see it again via the DPC UI, and the DPC team cannot recover it
   1. If you lose a client token, the only recovery is to generate a new one
   1. If you lose a client token, you should ask the DPC team to revoke that token
1. Upload a public key
   1. dpcclient can generate the public key you will need; see the `genKey` command
   1. Note the ID assigned to your public key; you will need it to get access tokens
1. To make an API request, you need an access token. To get an access token, you first need to generate an auth token.
   1. Documentation: [Authentication and Authorization](https://dpc.cms.gov/docs#authentication-and-authorization)
   1. dpcclient can generate the signed JWT you need; see the `genAuthToken` command
1. Make a request to the `/Token/auth` endpoint containing a correctly formed JWT, receiving an **access token** in response
   1. An **access token** expires after 5 minutes, so an application (or you whilst exploring the API) will need to repeat this step every 5 minutes
   1. For each request to the `/Token/auth` endpoint, you will also need to generate a new auth token as described in the previous step
   1. dpcclient can do this for you; see the `genAccessToken` command
1. Make a request to an application endpoint, using the access token generated in the previous step as a `Bearer` token.
   1. dpcclient does not make API endpoint requests (yet), so these have to be done with cURL, Postman, or a similar tool.
 
 # Commands

The general form of a dpcclient command is 

`dpcclient [command] [args] [flags]`. 

The command name is required. Commands that produce artifacts require a name for the artifact. (Yes, all current dpcclient commands produce artifacts.) Commands append additional information to the name you provide to produce the final name for their artifact. Each command describes how it uses the name you provide in the final name for the artifact it produces.

Help will always be given to those who ask for it like so: `dpcclient --help` or `dpcclient -h`. Help for a specific command is also available: `dpcclient genAuthToken -h`.

The following flags are global to dpcclient; all dpcclient commands understand them:

Flag           | Use
-------------- | ----
-k, --keydir   | the local directory where dpcclient stores keys it generates and looks for macaroons
-t, --tokendir | the local directory where dpcclient stores both auth and access tokens it generates
&nbsp;         | _Note that if values for keydir or tokendir are not specified, dpcclient will store and look for files in the current directory._<br> _In addition, you can use a .dpcclient.yaml in your home directory to set the keydir and tokendir values._                                      

Some dpcclient commands have additional flags, as follows:

Flag           | Use
-------------- | ----
-K, --keyname  | the PKI key name prefix provided by the user. dpcclient combines `-private.pem` with the key prefix for the private key file it generates, and `-public.pem` with the key prefix for the public key file it generates.
-I, --kid      | the ID DPC assigned to the public key you uploaded to it
-M, --macaroon | the name of a file containing the DPC **client token** (macaroon). dpcclient assumes the macaroon file is in the keydir. If it can't find it there, it will look in the current directory. If it can't find it there, it will complain and exit.

Command documentation is provided below. The help message provided by the executable should always be considered current if it happens to conflict with this documentation.

Command | Example
------- | -------
genKey | `dpcclient genKey myDPCSandboxKey -k ./keys` <br><br>Generates a correctly-sized RSA private key and its public key mate, saving them as .pem files in the indicated directory. Files are named by appending `-private.pem` or `-public.pem` to the name argument. For the example command, dpcclient would generate files named `myDPCSandboxKey-private.pem` and `myDPCSandboxKey-public.pem` in a directory named 'keys'.
genAuthToken | `dpcclient genAuthToken myDPCSandboxAuthToken -k ./keys -K myDPCSandboxKey -I <uuid> -M myDPCSandboxMacaroon -t ./tokens` <br><br> Generates a signed JWT required to obtain an **access token**, saving it in the tokens directory. The file is named by appending `-<unix timestamp>.jwt` to the name argument. The auth token file resulting from the example command would be named `myDPCAuthToken-1577976362.jwt` Unix timestamps are discussed [here](https://www.unixtimestamp.com).
getAccessToken | `dpcclient getAccessToken myDPCSandboxAccessToken -t ./tokens -A myDPCSandboxAuthToken-1577976362` <br><br> Sends a request to the DPC API /Token endpoint, receiving an access token in response. If the API responds with a 200, dpcclient will save the access token to a file named by the -T argument with `-<unix timestamp>.jwt` appended to it. If an error occurs, dpcclient will echo the error message to the terminal before exiting.

# All the Things

To illustrate the use of dpcclient, we include this walkthrough of the author using it.

Build executable:

```
go build
```

Make output directories:

```
mkdir out/keys
mkdir out/tokens
```

Create a .dpcclient.yaml file in my home directory:

```
touch ~/.dpcclient.yaml
```

Add these lines to it:

```
keydir: ./out/keys
tokendir: ./out/tokens
```

Generate a key pair:

```
dpcclient genKey happySandbox
ls -l ./out/keys
```

Upload the public key in the web UI, noting the ID assigned to it.
Get a client token from the web UI.
Save the client token (macaroon) in the ./out/keys directory

Generate an auth token and see it in the location I specified in the config file:

```
dpcclient genAuthToken happySandboxAuthToken -K happySandbox -I b3f7d972-9a91-432c-95b3-fabd085a2280 -M happySandboxMacaroon
ls -l ./out/tokens
```

Generate an access token and see it in the location I specified in the config file:

```
dpcclient getAccessToken happySandboxAccessToken -A happySandboxAuthToken-1585516859
ls -l ./out/tokens
```


Use the access token in a cURL request to the API:
```
curl --location --request GET 'http://sandbox.dpc.cms.gov/api/v1/Group' \
--header 'Accept: application/fhir+json' \
--header 'Prefer: respond-async' \
--header 'Authorization: Bearer <paste token here>'
```

Expected response:

```
{
    "resourceType": "Bundle",
    "type": "searchset",
    "total": 0
}
```
