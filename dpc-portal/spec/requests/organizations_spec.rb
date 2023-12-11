# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Organizations', type: :request do
  include DpcClientSupport

  describe 'GET /index' do
    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get '/organizations'
    end
  end

  describe 'GET /organizations/[organization_id]' do
    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get "/organizations/#{api_id}"
    end
  end
end
