# frozen_string_literal: true

require 'rails_helper'

describe OmniAuth::Strategies::OpenIDConnect do
  let(:idp_host) { 'idp.example.com' }
  let(:strategy) do
    described_class.new(nil, client_options: { host: idp_host, userinfo_endpoint: '/userinfo' })
  end
  let(:payload) do
    { 'sub' => '12345', 'email' => 'test@example.com' }
  end
  let(:rsa_key) { OpenSSL::PKey::RSA.generate(2048) }
  let(:jwk) { JSON::JWK.new(rsa_key) }

  def signed_jwt(claims, key: rsa_key, kid: jwk[:kid])
    token = JSON::JWT.new(claims)
    token.kid = kid
    token.sign(key, :RS256).to_s
  end

  describe '#user_info' do
    context 'when fetch_userinfo_payload succeeds' do
      before do
        allow(strategy).to receive(:fetch_userinfo_payload).and_return(payload)
      end

      it 'returns a UserInfo object' do
        result = strategy.user_info
        expect(result).to be_a(OpenIDConnect::ResponseObject::UserInfo)
      end

      it 'memoizes the result' do
        expect(strategy).to receive(:fetch_userinfo_payload).once
        strategy.user_info
        strategy.user_info
      end
    end

    context 'when fetch_userinfo_payload raises an error' do
      let(:error) { StandardError.new('something went wrong') }

      before do
        allow(strategy).to receive(:fetch_userinfo_payload).and_raise(error)
        allow(Rails.logger).to receive(:error)
        allow(strategy).to receive(:fail!)
      end

      it 'logs the error with the correct context' do
        expect(Rails.logger).to receive(:error).with(
          ['OIDC userinfo processing failed',
           hash_including(
             actionContext: LoggingConstants::ActionContext::Authentication,
             actionType: LoggingConstants::ActionType::OidcUserInfoFailed,
             exceptionClass: 'StandardError'
           )]
        )
        strategy.user_info
      end

      it 'calls fail! with the correct arguments' do
        expect(strategy).to receive(:fail!).with(:user_info_failed, error)
        strategy.user_info
      end

      it 'returns nil' do
        expect(strategy.user_info).to be_nil
      end
    end
  end

  describe '#decode_verified_jwt' do
    it 'delegates to OidcJwksVerifier using the client host' do
      jwt_string = signed_jwt(payload)
      expect(OidcJwksVerifier).to receive(:decode_and_verify)
        .with(jwt_string, host: idp_host).and_return(payload)

      expect(strategy.send(:decode_verified_jwt, jwt_string)).to eq(payload)
    end

    it 'propagates verification failures raised by OidcJwksVerifier' do
      allow(OidcJwksVerifier).to receive(:decode_and_verify).and_raise(JSON::JWT::VerificationFailed)

      expect { strategy.send(:decode_verified_jwt, signed_jwt(payload)) }
        .to raise_error(JSON::JWT::VerificationFailed)
    end
  end

  describe '#userinfo_endpoint_uri' do
    it 'builds the endpoint from host/scheme/port when userinfo_endpoint is a path' do
      expect(strategy.send(:userinfo_endpoint_uri)).to eq("https://#{idp_host}/userinfo")
    end

    it 'returns the endpoint unchanged when it is already an absolute URL' do
      strategy = described_class.new(
        nil, client_options: { host: idp_host, userinfo_endpoint: 'https://other.example.com/userinfo' }
      )
      expect(strategy.send(:userinfo_endpoint_uri)).to eq('https://other.example.com/userinfo')
    end
  end

  describe '#fetch_userinfo_payload' do
    let(:userinfo_uri) { "https://#{idp_host}/userinfo" }

    before do
      allow(strategy).to receive(:access_token).and_return(double(access_token: 'a-token'))
    end

    it 'verifies the signature via OidcJwksVerifier, returns the userinfo claims ' \
       'when Content-Type header indicates JWT in user info response payload' do
      stub_request(:get, userinfo_uri)
        .with(headers: { 'Authorization' => 'Bearer a-token' })
        .to_return(body: signed_jwt(payload), headers: { 'Content-Type' => 'application/jwt' })
      expect(OidcJwksVerifier).to receive(:decode_and_verify)
        .with(signed_jwt(payload), host: idp_host).and_return(payload)

      result = strategy.send(:fetch_userinfo_payload)
      expect(result).to include('sub' => '12345', 'email' => 'test@example.com')
    end

    it 'raises when OidcJwksVerifier rejects the userinfo JWT' do
      tampered_jwt = signed_jwt(payload, key: OpenSSL::PKey::RSA.generate(2048))
      stub_request(:get, userinfo_uri)
        .with(headers: { 'Authorization' => 'Bearer a-token' })
        .to_return(status: 200, body: tampered_jwt, headers: { 'Content-Type' => 'application/jwt' })
      allow(OidcJwksVerifier).to receive(:decode_and_verify).and_raise(JSON::JWT::VerificationFailed)

      expect { strategy.send(:fetch_userinfo_payload) }.to raise_error(JSON::JWT::VerificationFailed)
    end
  end
end
