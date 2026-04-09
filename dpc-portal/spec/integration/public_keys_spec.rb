# frozen_string_literal: true

require 'base64'
require 'openssl'
require 'rails_helper'
require 'support/login_support'

RSpec.describe 'PublicKeys', type: :request do
  include LoginSupport

  before(:example) { WebMock.disable_net_connect!(allow_localhost: true, allow: ['api']) }
  after(:example) { WebMock.disable_net_connect!(allow_localhost: true) }

  describe 'Public Keys', :integration do
    let(:dpc_api_organization_id) { SecureRandom.uuid }
    let!(:user) { create(:user) }
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
    let!(:link) { create(:cd_org_link, user:, provider_organization: org) }
    let(:label) { 'New Public Key' }
    before do
      org.update!(terms_of_service_accepted_by: user)
      sign_in user
    end
    it 'should generate a public key, show on org page, and delete it' do
      get "/organizations/#{org.id}"
      expect(response.body).to include('You have no public keys.')
      expect(response.body).to_not include(label)

      rsa_key = OpenSSL::PKey::RSA.new(4096)
      public_key = rsa_key.public_key.to_pem
      message = 'This is the snippet used to verify a key pair in DPC.'
      digest = OpenSSL::Digest.new('SHA256')
      signature_binary = rsa_key.sign(digest, message)
      snippet_signature = Base64.encode64(signature_binary)
      params = { label:, public_key:, snippet_signature: }

      post "/organizations/#{org.id}/public_keys", params: params
      expect(response).to redirect_to(organization_path(org, credential_start: true))
      expect(assigns(:organization)).to eq org
      expect(flash[:success]).to eq('Public key created successfully.')

      get "/organizations/#{org.id}"
      expect(response.body).to_not include('You have no public keys.')
      expect(response.body).to include(label)

      delete_path_match = %r{action="(/organizations/#{org.id}/public_keys/[^"]+)}.match(response.body)
      expect(delete_path_match).to be_truthy

      delete delete_path_match[1]

      expect(flash[:success]).to eq('Public key deleted successfully.')
      expect(response).to redirect_to(organization_path(org, credential_start: true))

      get "/organizations/#{org.id}"
      expect(response.body).to include('You have no public keys.')
      expect(response.body).to_not include(label)
    end
  end
end
