# frozen_string_literal: true

require 'spec_helper'
require 'rails_helper'
require 'support/login_support'

describe UserInfoService do
  let(:service) { UserInfoService.new }
  let(:token) { 'bearer-token' }
  let(:exp) { 2.hours.from_now }
  LoginSupport::CSP_MAP.each do |provider, display_name|
    context "with #{display_name}" do
      context :valid_session do
        before do
          stub_request(:get, user_info_url(provider))
            .with(headers: { Authorization: "Bearer #{token}" })
            .to_return(body: csp_response(provider).to_json, status: 200)
        end

        it 'should return info with valid session' do
          verify_logs(status: 200, csp: provider)
          expect(service.user_info(build_csp_session(provider))).to eq csp_response(provider)
        end
      end

      context :bad_request do
        it 'should throw error if status is 401' do
          verify_logs(status: 401, csp: provider)
          error = '{"error":"No can do"}'
          stub_request(:get, user_info_url(provider))
            .with(headers: { Authorization: "Bearer #{token}" })
            .to_return(body: error, status: 401)
          expect do
            service.user_info(build_csp_session(provider))
          end.to raise_error(UserInfoServiceError, 'unauthorized')
        end
        it 'should throw error if status is 500' do
          verify_logs(status: 500, csp: provider)
          error = '{"error":"shrug"}'
          stub_request(:get, user_info_url(provider))
            .with(headers: { Authorization: "Bearer #{token}" })
            .to_return(body: error, status: 500)
          expect do
            service.user_info(build_csp_session(provider))
          end.to raise_error(UserInfoServiceError, 'server_error')
        end
        it 'should throw error if cannot connect' do
          verify_logs(status: 503, csp: provider, connection_fails: true)
          stub_request(:get, user_info_url(provider))
            .with(headers: { Authorization: "Bearer #{token}" })
            .to_raise(Errno::ECONNREFUSED)
          expect do
            service.user_info(build_csp_session(provider))
          end.to raise_error(UserInfoServiceError, 'server_error')
        end
      end

      context :invalid_session do
        it 'should throw error if no token' do
          csp_session_with_nil_token = build_csp_session(provider, token: nil)
          expect do
            service.user_info(csp_session_with_nil_token)
          end.to raise_error(UserInfoServiceError, 'no_token')
        end
        it 'should throw error if no token expiration' do
          csp_session_with_nil_token_exp = build_csp_session(provider, token_exp: nil)
          expect do
            service.user_info(csp_session_with_nil_token_exp)
          end.to raise_error(UserInfoServiceError, 'no_token_exp')
        end
        context :expired_session do
          let(:exp) { 1.second.ago }
          it 'should throw error' do
            expect do
              service.user_info(build_csp_session(:login_dot_gov))
            end.to raise_error(UserInfoServiceError, 'expired_token')
          end
        end
      end
    end
  end
  def verify_logs(status:, csp:, connection_fails: false)
    verify_dd(csp:, connection_fails:)
    verify_rails(status: status, csp: csp) unless connection_fails
  end

  def verify_dd(csp:, connection_fails:)
    span = instance_double(Datadog::Tracing::Span)
    expect_span_tags(span, csp:, connection_fails:)
    expect(Datadog::Tracing).to receive(:trace)
      .with('user_info_service.request', resource: 'request_info') do |&block|
      block.call(span)
    end
  end

  def expect_span_tags(span, csp:, connection_fails:)
    expect_http_metadata(span, csp)
    expect(span).to receive(:set_tag).with('http.status_code', anything) unless connection_fails
  end

  def expect_http_metadata(span, csp)
    expect(span).to receive(:type=).with('http')
    expect(span).to receive(:set_tag).with('http.url', user_info_url(csp))
    expect(span).to receive(:set_tag).with('http.method', 'GET')
  end

  def verify_rails(status:, csp:)
    allow(Rails.logger).to receive(:info)
    verify_call_log(csp:)
    verify_response_log(status:, csp:)
  end

  def verify_call_log(csp:)
    expect(Rails.logger).to receive(:info).with(
      ['Calling CSP user_info',
       { csp: csp.to_s,
         csp_request_method: :get,
         csp_request_url: user_info_url(csp),
         csp_request_method_name: :request_info }]
    )
  end

  def verify_response_log(status:, csp:)
    expect(Rails.logger).to receive(:info).with(
      ['CSP user_info response info',
       { csp: csp.to_s,
         csp_request_method: :get,
         csp_request_url: user_info_url(csp),
         csp_request_method_name: :request_info,
         csp_response_status_code: status,
         csp_response_duration: anything }]
    )
  end

  def build_csp_session(csp, token: self.token, token_exp: exp)
    rails_session = ActiveSupport::HashWithIndifferentAccess.new
    cs = CspSession.new(rails_session)
    cs.store(csp: csp, token: token, token_exp: token_exp)
    cs
  end

  def csp_response(csp)
    file_path_components = ['csps', csp.to_s, 'user_info.json']
    file_path = File.join(*file_path_components)
    json_fixture(file_path)
  end

  def user_info_url(csp)
    case csp.to_s
    when 'login_dot_gov' then LOGIN_DOT_GOV_CLIENT_CONFIG[:client_options][:userinfo_endpoint]
    when 'id_me' then ID_ME_CLIENT_CONFIG[:client_options][:userinfo_endpoint]
    # when 'clear' then CspConfig::CLEAR.user_info_endpoint
    else raise ArgumentError, "Unknown CSP code: #{csp}"
    end
  end
end
