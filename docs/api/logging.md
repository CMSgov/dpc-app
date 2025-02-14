# API Logging

The API services log in JSON format, customized using the [DPCJsonLayoutBaseFactory](/dpc-common/src/main/java/gov/cms/dpc/common/logging/DPCJsonLayoutBaseFactory.java). The application.yml files configure certain globally-logged fields using the `additionalFields` key.

Note that each API configuration file sets two loggers, one for access logs and one for general usage in the Dropwizard application. Therefore, they may differ slightly in which keys are being logged.

## Request-local values

The Mapped Diagnostic Context (MDC) is an slf4j construct which allows us to store key-value pairs that are maintained for the lifetime of the request. Dropwizard automatically includes these values in all logs under the `mdc` object.

The [MDCConstants](/dpc-common/src/main/java/gov/cms/dpc/common/MDCConstants.java) class maintains a centralized list of all MDC global keys we are utilizing for logging. These are set at various points within the applications (using `MDC.put`) to include as much context as possible about the request and make debugging and data investigations easier.
