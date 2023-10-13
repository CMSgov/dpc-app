# frozen_string_literal: true

class FhirResourceBuilder
  def fhir_org(org, api_id, api_endpoint_ref)
    fhir_org = FHIR::Organization.new(
      id: api_id,
      name: org.name,
      identifier: [
        {
          system: 'http://hl7.org/fhir/sid/us-npi',
          value: org.npi
        }
      ]
    )
    fhir_org.endpoint = { reference: api_endpoint_ref }

    fhir_org.address = fhir_address(org)
    fhir_org
  end

  def fhir_address(org)
    FHIR::Address.new(
      line: org.address_street,
      city: org.address_city,
      postalCode: org.address_zip,
      state: org.address_state,
      country: 'US',
      use: org.address_use,
      type: org.address_type
    )
  end

  def fhir_endpoint(api_id, fhir_endpoint_id, fhir_endpoint)
    FHIR::Endpoint.new(
      id: fhir_endpoint_id,
      status: fhir_endpoint.status,
      name: fhir_endpoint.name,
      address: fhir_endpoint.uri,
      managingOrganization: { reference: "Organization/#{api_id}" },
      payloadType: payload_type,
      connectionType: connection_type
    )
  end

  private

  def connection_type
    {
      system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type',
      code: 'hl7-fhir-rest'
    }
  end

  def payload_type
    [
      {
        'coding': [
          {
            'system': 'http://hl7.org/fhir/endpoint-payload-type',
            'code': 'any'
          }
        ]
      }
    ]
  end
end
