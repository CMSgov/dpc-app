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

How to start the DPCApp application
---

1. Run `mvn clean install` to build your application
1. Start application with `java -jar target/dpc-app-1.0-SNAPSHOT.jar server config.yml`
1. To check that your application is running enter url `http://localhost:3002`

Health Check
---

To see your applications health enter url `http://localhost:9900/healthcheck`
