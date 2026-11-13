# DPC Attribution Client

Client for connecting with the dpc-attribution service.  

All connections to dpc-attribution
should go through this library.  It allows us abstract away the details of connecting to dpc-attribution
making testing the calling services easier, our eventual move to shared attribution *much* easier, and it's just good design.

### Using dpc-attribution-client
Bind the AttributionClientModule to the calling service's Guice bundle and then Guice can inject it wherever it's
needed.
