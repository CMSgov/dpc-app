# frozen_string_literal: true

require 'rails_helper'
require 'securerandom'
require 'support/csp_auth_shared_examples'

RSpec.describe 'Clear', type: :request do
  let(:uuid) { SecureRandom.uuid }
  describe 'POST /auth/clear' do
    let!(:csp) { Csp.find_by(name: 'clear') || create(:csp, :clear) }
    let(:token) { 'bearer-token' }
    let(:id_token) { 'id-token' }
    let(:csp_auth_response) do
      { uid: uuid,
        credentials: { expires_in: 899,
                       token:,
                       id_token: },
        info: { email: 'bob2@example.com' },
        extra: { raw_info: { sub: uuid,
                             email: 'bob2@example.com',
                             given_name: 'Bob',
                             family_name: 'Hoskins',
                             SSN: '123456789',
                             ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } }
    end

    clear_config = {
      provider: :clear,
      auth_endpoint: '/auth/clear',
      display_name: 'CLEAR',
      logout_host: ENV.fetch('CLEAR_IDP_HOST'),
      expected_id_token: 'id-token' # covers store_id_token = true for clear_controller
    }
    it_behaves_like 'a CSP client', clear_config

    context 'should add emails' do
      before do
        uuid = SecureRandom.uuid
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:clear,
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token:,
                                                  id_token: },
                                   info: { email: 'email1@example.com' },
                                   extra: { raw_info: { sub: uuid,
                                                        email: 'email1@example.com',
                                                        given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        SSN: '123456789',
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })

        user = create(:user)
        create(:csp_user, user:, uuid:, csp:)
      end

      it 'adds emails' do
        expect do
          post '/auth/clear'
          follow_redirect!
        end.to change { UserEmail.count }.by(1)

        email = UserEmail.last
        expect(email.email).to eq 'email1@example.com'
        expect(email.active).to eq true
        expect(email.primary).to eq true
      end
    end

    context 'should deactivate emails' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:clear,
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token:,
                                                  id_token: },
                                   info: { email: 'email1@example.com' },
                                   extra: { raw_info: { sub: uuid,
                                                        email: 'email1@example.com',
                                                        given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        SSN: '123456789',
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })

        user = create(:user)
        csp_user = create(:csp_user, user:, uuid:, csp:)
        create(:user_email, csp_user:, email: 'email@example.com', active: true)
      end

      it 'deactivates email' do
        post '/auth/clear'
        follow_redirect!

        email = UserEmail.find_by(csp_user: CspUser.last, email: 'email@example.com')
        expect(email.active).to eq false
        expect(email.deactivated_at).to_not be_nil
        expect(email.reactivated_at).to be_nil
      end
    end

    context 'should reactivate emails' do
      before do
        OmniAuth.config.test_mode = true
        OmniAuth.config.add_mock(:clear,
                                 { uid: uuid,
                                   credentials: { expires_in: 899,
                                                  token:,
                                                  id_token: },
                                   info: { email: 'email1@example.com' },
                                   extra: { raw_info: { sub: uuid,
                                                        email: 'email1@example.com',
                                                        given_name: 'Bob',
                                                        family_name: 'Hoskins',
                                                        SSN: '123456789',
                                                        ial: 'http://idmanagement.gov/ns/assurance/ial/2' } } })

        user = create(:user)
        csp_user = create(:csp_user, user:, uuid:, csp:)
        create(:user_email, csp_user:, email: 'email1@example.com', active: false, deactivated_at: 1.day.ago,
                            reactivated_at: nil)
      end

      it 'reactivates emails' do
        post '/auth/clear'
        follow_redirect!

        email = UserEmail.find_by(csp_user: CspUser.last, email: 'email1@example.com')
        expect(email.active).to eq true
        expect(email.deactivated_at).to be_nil
        expect(email.reactivated_at).to_not be_nil
        expect(email.primary).to eq true
      end
    end
  end
end
