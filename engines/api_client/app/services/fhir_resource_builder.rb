# frozen_string_literal: true

# builds payloads for fhir resources
class FhirResourceBuilder
  def fhir_org(org, api_id)
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
end
