class OrganizationSubmitSerializer < ActiveModel::Serializer
  attribute(:resourceType) { 'Parameters' }

  attribute(:parameter) do
    [
      {
        name: 'resource',
        resource: {
          resourceType: 'Bundle',
          type: 'collection',
          entry: [
            organization_resource,
            endpoint_resource
          ]
        }
      }
    ]
  end

  def organization_resource
    {
      resource: {
        address: [
          {
            use: object.address_use,
            type: object.address_type,
            city: object.address_city,
            country: 'US',
            line: [
              object.address_street,
              object.address_street_2
            ],
            postalCode: object.address_zip,
            state: object.address_state
          }
        ],
        identifier: [
          {
            system: 'http://hl7.org/fhir/sid/us-npi',
            value: object.npi
          }
        ],
        name: object.name,
        resourceType: 'Organization',
        type: [
          {
            coding: [
              {
                code: 'prov',
                display: 'Healthcare Provider',
                system: 'http://hl7.org/fhir/organization-type'
              }
            ],
            text: 'Healthcare Provider'
          }
        ]
      }
    }
  end

  def endpoint_resource
    {
      resource: {
        resourceType: 'Endpoint',
        status: object.fhir_endpoint_status,
        connectionType: {
          system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type',
          code: 'hl7-fhir-rest'
        },
        name: object.fhir_endpoint_name,
        address: object.fhir_endpoint_uri
      }
    }
  end
end
