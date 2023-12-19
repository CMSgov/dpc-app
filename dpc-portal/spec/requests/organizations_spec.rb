# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Organizations', type: :request do
  include DpcClientSupport

  describe 'GET /index' do
    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_api_client(message: :get_organization,
                      response: default_get_org_response(api_id))
      get '/organizations', params: { id: api_id }
    end
  end

  describe 'GET /organizations/[organization_id]' do
    it 'returns success' do
      api_id = SecureRandom.uuid
      stub_client(api_id)
      get "/organizations/#{api_id}"
      expect(assigns(:organization).api_id).to eq api_id
    end

    it 'redirects if prod-sbx' do
      api_id = SecureRandom.uuid
      stub_const('ENV', ENV.to_hash.merge('ENV' => 'prod-sbx'))
      get "/organizations/#{api_id}"
      expect(response).to redirect_to(root_url)
    end

    it 'goes to hard-coded org if test' do
      api_id = SecureRandom.uuid
      stub_const('ENV', ENV.to_hash.merge('ENV' => 'test'))
      hard_coded_id = '6a1dbf47-825b-40f3-b81d-4a7ffbbdc270'
      stub_client(hard_coded_id)
      get "/organizations/#{api_id}"
      expect(assigns(:organization).api_id).to eq hard_coded_id
    end

    it 'goes to hard-coded org if dev' do
      api_id = SecureRandom.uuid
      stub_const('ENV', ENV.to_hash.merge('ENV' => 'dev'))
      hard_coded_id = '78d02106-2837-4d07-8c51-8d73332aff09'
      stub_client(hard_coded_id)
      get "/organizations/#{api_id}"
      expect(assigns(:organization).api_id).to eq hard_coded_id
    end
  end

  def stub_client(api_id)
    client = stub_api_client(message: :get_organization,
                             response: default_get_org_response(api_id))
    stub_self_returning_api_client(message: :get_client_tokens,
                                   response: default_get_client_tokens,
                                   api_client: client)
    stub_self_returning_api_client(message: :get_public_keys,
                                   response: default_get_public_keys,
                                   api_client: client)
  end
end
