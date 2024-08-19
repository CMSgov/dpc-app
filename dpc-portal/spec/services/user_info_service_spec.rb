# frozen_string_literal: true

require 'spec_helper'
require 'rails_helper'

describe UserInfoService do
  let(:user_info_url) { UserInfoService::USER_INFO_URI }
  let(:service) { UserInfoService.new }
  let(:token) { 'bearer-token' }
  let(:exp) { 2.hours.from_now }
  let(:valid_session) { { login_dot_gov_token: token, login_dot_gov_token_exp: exp } }

  context :valid_session do
    let(:response) do
      {
        'sub' => '097d06f7-e9ad-4327-8db3-0ba193b7a2c2',
        'iss' => 'https://idp.int.identitysandbox.gov/',
        'email' => 'david@example.com',
        'email_verified' => true,
        'all_emails' => [
          'david@example.com',
          'david2@example.com'
        ],
        'given_name' => 'David',
        'family_name' => 'Davis',
        'birthdate' => '1938-10-06',
        'social_security_number' => '900888888',
        'phone' => '+19174216435',
        'phone_verified' => true,
        'verified_at' => 1_704_834_157,
        'ial' => 'http://idmanagement.gov/ns/assurance/ial/2',
        'aal' => 'urn:gov:gsa:ac:classes:sp:PasswordProtectedTransport:duo'
      }
    end

    before do
      stub_request(:get, user_info_url)
        .with(headers: { Authorization: "Bearer #{token}" })
        .to_return(body: response.to_json, status: 200)
    end

    it 'should return info with valid session' do
      verify_logs(status: 200)
      expect(service.user_info(valid_session)).to eq response
    end
  end

  context :bad_request do
    it 'should throw error if status is 401' do
      verify_logs(status: 401)
      error = '{"error":"No can do"}'
      stub_request(:get, user_info_url)
        .with(headers: { Authorization: "Bearer #{token}" })
        .to_return(body: error, status: 401)
      expect do
        service.user_info(valid_session)
      end.to raise_error(UserInfoServiceError, 'unauthorized')
    end
    it 'should throw error if status is 500' do
      verify_logs(status: 500)
      error = '{"error":"shrug"}'
      stub_request(:get, user_info_url)
        .with(headers: { Authorization: "Bearer #{token}" })
        .to_return(body: error, status: 500)
      expect do
        service.user_info(valid_session)
      end.to raise_error(UserInfoServiceError, 'server_error')
    end
    it 'should throw error if cannot connect' do
      verify_logs(status: 503)
      stub_request(:get, user_info_url)
        .with(headers: { Authorization: "Bearer #{token}" })
        .to_raise(Errno::ECONNREFUSED)
      expect do
        service.user_info(valid_session)
      end.to raise_error(UserInfoServiceError, 'server_error')
    end
  end

  context :invalid_session do
    it 'should throw error if no token' do
      invalid = valid_session.merge(login_dot_gov_token: nil)
      expect do
        service.user_info(invalid)
      end.to raise_error(UserInfoServiceError, 'no_token')
    end
    it 'should throw error if no token expiration' do
      invalid = valid_session.merge(login_dot_gov_token_exp: nil)
      expect do
        service.user_info(invalid)
      end.to raise_error(UserInfoServiceError, 'no_token_exp')
    end
    context :expired_session do
      let(:exp) { 1.second.ago }
      it 'should throw error' do
        expect do
          service.user_info(valid_session)
        end.to raise_error(UserInfoServiceError, 'expired_token')
      end
    end
  end
  def verify_logs(status:)
    allow(Rails.logger).to receive(:info)
    expect(Rails.logger).to receive(:info).with(
      ['Calling Login.gov user_info',
       { login_dot_gov_request_method: :get,
         login_dot_gov_request_url: user_info_url,
         login_dot_gov_request_method_name: :request_info }]
    )
    expect(Rails.logger).to receive(:info).with(
      ['Login.gov user_info response info',
       { login_dot_gov_request_method: :get,
         login_dot_gov_request_url: user_info_url,
         login_dot_gov_request_method_name: :request_info,
         login_dot_gov_response_status_code: status,
         login_dot_gov_response_duration: anything }]
    )
  end
end
