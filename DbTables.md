Database Tables Setup
---

Normally during the DPC setup, all the necessary tables are populated automatically. If, however, you're not yet authorized 
to access the DPC GitHub account, calls to the DPC endpoints will fail. In these instances, use the example tables
below to populate the necessary tables. Once completed, access to the DPC endpoints should function normally.

### Required Values:
- **Organization ID**: One can generate a UUID for this value using online tools.
- **NPI** (National Provider Identification): You can use any of the online NPI generators such as this one on [JSFiddle](https://jsfiddle.net/alexdresko/cLNB6/).


### dpc-website_development_REGISTERED_ORGANIZATIONS

| **column_name**  | **data_type**               | **Value**                                     | **Comment**                                        |
|------------------|-----------------------------|-----------------------------------------------|----------------------------------------------------|
| id               | bigint                      |                                             1 |                                                    |
| organization_id  | integer                     |                                             1 |                                                    |
| api_id           | character varying           | b14f8e17-dfc2-469a-9e63-3b8550e4bb77          | Same as OrganizationID, dpc_attribute.organization |
| created_at       | timestamp without time zone |                           2023-02-08 17:31:05 |                                                    |
| updated_at       | timestamp without time zone |                           2023-02-08 17:31:05 |                                                    |
| api_endpoint_ref | character varying           | Endpoint/b1a8394f-7026-431f-a823-e7b7f4c8c783 | Endpoint/'+(api_id in cell# A4)                    |
| enabled          | boolean                     |                      TRUE                     |                                                    |


### dpc-website_development_ORGANIZATIONS

| **column_name**   | **data_type**               | **value**           | **comment**                                     |
|-------------------|-----------------------------|---------------------|-------------------------------------------------|
| id                | bigint                      | 1                   |                                                 |
| name              | character varying           | My Care Org.        |                                                 |
| organization_type | integer                     | 0                   |                                                 |
| num_providers     | integer                     | 1                   |                                                 |
| created_at        | timestamp without time zone | 2023-02-08 17:30:49 |                                                 |
| updated_at        | timestamp without time zone | 2023-02-08 17:30:49 |                                                 |
| npi               | character varying           | 1417216748          | Same as dpc_attributes.organizations's id_value |
| vendor            | character varying           | My Care Org.        |                                                 |

### dpc-website_development_ORGANIZATION_USER_ASSIGNMENTS

| **column_name** | **data_type**               | **value**           | **comment** |
|-----------------|-----------------------------|---------------------|-------------|
| id              | bigint                      |                   1 |             |
| organization_id | integer                     |                   1 |             |
| user_id         | integer                     |                   1 |             |
| created_at      | timestamp without time zone | 2023-02-08 17:30:49 |             |
| updated_at      | timestamp without time zone | 2023-02-08 17:30:49 |             |

### dpc-website_development_FHIR_ENDPOINTS

| column_name                | data_type         | value            |
|----------------------------|-------------------|------------------|
| id                         | bigint            |                1 |
| name                       | character varying | test             |
| status                     | integer           |                0 |
| uri                        | character varying | https://test.com |
| organization_id            | integer           | null             |
| registered_organization_id | integer           |                1 |

### dpc_attributions_ORGANIZATION

| **column_name**   | **data_type**     | **value**                            | **comment**   |
|-------------------|-------------------|--------------------------------------|---------------|
| id                | uuid              | b14f8e17-dfc2-469a-9e63-3b8550e4bb77 | autogenerated |
| id_system         | smallint          | 1                                    |               |
| id_value          | character varying | 1417216748                           | NPI           |
| organization_name | character varying | My Care Org.                         |               |
| address_use       | smallint          | 1                                    |               |
| address_type      | smallint          | 0                                    |               |
| line1             | character varying | 4823 Saginaw St                      |               |
| line2             | character varying | null                                 |               |
| city              | character varying | Flint                                |               |
| district          | character varying | null                                 |               |
| state             | character varying | MI                                   |               |
| postal_code       | character varying | 48503                                |               |
| country           | character varying | US                                   |               |



### dpc_attributions_ORGANIZATION_ENDPOINT

| **column_name**    | **data_type**     | **value**                                                      | **comment**              |
|--------------------|-------------------|----------------------------------------------------------------|--------------------------|
| id                 | uuid              | b1a8394f-7026-431f-a823-e7b7f4c8c783                           | autogenerated            |
| organization_id    | uuid              | b14f8e17-dfc2-469a-9e63-3b8550e4bb77                           | from organizations table |
| status             | smallint          |                                                              5 |                          |
| system             | character varying | http://terminology.hl7.org/CodeSystem/endpoint-connection-type |                          |
| code               | character varying | hl7-fhir-rest                                                  |                          |
| name               | character varying | testing 123                                                    |                          |
| address            | character varying | https://test.com                                               |                          |
| validation_status  | smallint          |                                                              0 |                          |
| validation_message | character varying |                                                                |                          |
