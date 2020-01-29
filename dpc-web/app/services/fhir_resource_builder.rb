# frozen_string_literal: true

class FhirResourceBuilder
  def fhir_org(reg_org)
    org = reg_org.organization
    fhir_org = FHIR::Organization.new(
      id: reg_org.api_id,
      name: org.name,
      identifier: [{ system: 'http://hl7.org/fhir/sid/us-npi', value: org.npi }]
    )
    fhir_org.endpoint = { reference: reg_org.api_endpoint_ref }

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

  def fhir_endpoint(reg_org)
    fhir_endpoint_id = reg_org.api_endpoint_ref.split('/')[1]
    fhir_endpoint = reg_org.fhir_endpoint
    FHIR::Endpoint.new(
      id: fhir_endpoint_id,
      status: fhir_endpoint.status,
      name: fhir_endpoint.name,
      address: fhir_endpoint.uri,
      managingOrganization: { reference: "Organization/#{reg_org.api_id}" },
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
