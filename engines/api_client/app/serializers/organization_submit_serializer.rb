# frozen_string_literal: true

# Turns object into hash
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
            endpoint_resources
          ].flatten
        }
      }
    ]
  end

  def organization_resource
    {
      resource: {
        address: [address],
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

  def address
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
  end

  def endpoint_resources
    [endpoint_resource(instance_options[:fhir_endpoint])]
  end

  def endpoint_resource(fhir_endpoint)
    {
      resource: {
        resourceType: 'Endpoint',
        status: fhir_endpoint['status'],
        connectionType: {
          system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type',
          code: 'hl7-fhir-rest'
        },
        payloadType: [
          {
            coding: [
              {
                system: 'http://hl7.org/fhir/endpoint-payload-type',
                code: 'any'
              }
            ]
          }
        ],
        name: fhir_endpoint['name'],
        address: fhir_endpoint['uri']
      }
    }
  end
end
