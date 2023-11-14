# Seeding the Database

**Note**: This step is not required when directly running the `demo` for the `dpc-api` service, which partially seeds the database on first execution.

By default, DPC initially starts with an empty attribution database, which means that no patients have been attributed to any providers and thus nothing can be exported from BlueButton2.0.

In order to successfully test and demonstrate the application, there needs to be initial data loaded into the attribution database.
We provide a small CSV [file](src/main/resources/test_associations.csv) which associates some fake providers with valid patients from the BlueButton2.0 Sandbox.

The database can be automatically migrated and seeded by running `make seed-db` or by using the following commands:

**Note:** For instances where one cannot set up the DPC due to authorization issues, follow the steps in the [manual table setup document](DbTables.md) to populate the necessary tables manually.

```bash
java -jar dpc-attribution/target/dpc-attribution.jar db migrate
java -jar dpc-attribution/target/dpc-attribution.jar seed
```
