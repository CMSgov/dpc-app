# frozen_string_literal: true

require 'rails_helper'

RSpec.describe FhirEndpoint, type: :model do
  describe 'validations' do
    describe 'uri_is_valid_format' do
      it 'allows a valid URI' do
        uri = 'https://example.com/endpoint'
        fhir_endpoint = build(:fhir_endpoint, uri:)
        expect(fhir_endpoint).to be_valid
      end

      it 'adds an error if URI is invalid' do
        uri = 'invalid`uri-bad-!!'
        fhir_endpoint = build(:fhir_endpoint, uri:)
        expect(fhir_endpoint).not_to be_valid
        expect(fhir_endpoint.errors.full_messages).to eq(['Uri must be valid URI (must have valid domain name)'])
      end
    end
  end
end
