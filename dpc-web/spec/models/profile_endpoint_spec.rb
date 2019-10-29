# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ProfileEndpoint, type: :model do
  describe 'validations' do
    describe 'uri_is_valid_format' do
      it 'allows a valid URI' do
        uri = 'https://example.com/endpoint'
        profile_endpoint = build(:profile_endpoint, uri: uri)
        expect(profile_endpoint).to be_valid
      end

      it 'adds an error if URI is invalid' do
        uri = 'invalid`uri-bad-!!'
        profile_endpoint = build(:profile_endpoint, uri: uri)
        expect(profile_endpoint).not_to be_valid
        expect(profile_endpoint.errors.full_messages).to eq(['Uri must be valid URI'])
      end
    end
  end
end
