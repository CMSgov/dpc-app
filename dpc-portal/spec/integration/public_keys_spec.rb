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
    let!(:org) { create(:provider_organization, dpc_api_organization_id:, name: 'Health Hut') }
    let(:label) { 'New Public Key' }

    CspUtils::CODES_TO_DISPLAY.each do |provider, display_name|
      context "using #{display_name}" do
        before do
          user = create_user_with_csp(csp: provider)
          create(:cd_org_link, user:, provider_organization: org)
          org.update!(terms_of_service_accepted_by: user)
          sign_in user, csp: provider
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

        it 'redirects with alert when id contains path traversal' do
          delete "/organizations/#{org.id}/public_keys/../../etc/passwd"
          expect(flash[:alert]).to eq('Public key could not be deleted.')
          expect(response).to redirect_to(organization_path(org, credential_start: true))
        end

        it 'redirects with alert when id contains special characters' do
          delete "/organizations/#{org.id}/public_keys/<script>alert(1)</script>"
          expect(flash[:alert]).to eq('Public key could not be deleted.')
          expect(response).to redirect_to(organization_path(org, credential_start: true))
        end

        it 'redirects with alert when id is blank' do
          delete "/organizations/#{org.id}/public_keys/%20"
          expect(flash[:alert]).to eq('Public key could not be deleted.')
          expect(response).to redirect_to(organization_path(org, credential_start: true))
        end

        it 'redirects with alert when id exceeds 64 characters' do
          long_id = 'a' * 65
          delete "/organizations/#{org.id}/public_keys/#{long_id}"
          expect(flash[:alert]).to eq('Public key could not be deleted.')
          expect(response).to redirect_to(organization_path(org, credential_start: true))
        end

        it 'does not call the API when id is invalid' do
          expect_any_instance_of(PublicKeyManager).not_to receive(:delete_public_key)
          delete "/organizations/#{org.id}/public_keys/../../etc/passwd"
        end
      end
    end
  end
end
