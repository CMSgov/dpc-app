# Data @ The Point of Care

Clone and Build Submodules
---

First-time clone:

```bash
git clone --recursive https://github.com/CMSgov/dpc-app
```

Or, to pull submodules into existing repository:

```bash
git submodule init
git submodule update
```

Build with makefile:
```bash
Make
```

TLS configuration for BlueButtonClient
---

First, back up your JDK's cacerts file:
```bash
cp $JAVA_HOME/lib/security/cacerts $JAVA_HOME/lib/security/cacerts.backup
```

Next, retrieve the bluebutton development server's self-signed certificate and add it to the jdk's trust store:

```bash
echo -n | openssl s_client -connect fhir.backend.bluebutton.hhsdevcloud.us:443 -servername fhir.backend.bluebutton.hhsdevcloud.us -cert client-test-keypair.pem \\n| sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' \\n| tee "/tmp/server.crt"
sudo keytool -import -cacerts -storepass changeit -alias bb-dev-selfsigned -file /tmp/server.crt
```

Finally, install the keystore with the client certificate here:
```
$HOME/.keystore
```

How to start the DPCApp application
---

1. Run `mvn clean install` to build your application
1. Start application with `java -jar target/dpc-app-1.0-SNAPSHOT.jar server config.yml`
1. To check that your application is running enter url `http://localhost:3002`

Health Check
---

To see your applications health enter url `http://localhost:9900/healthcheck`
