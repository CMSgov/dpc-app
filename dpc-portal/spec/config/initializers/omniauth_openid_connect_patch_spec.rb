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

  before do
    stub_const('OmniAuth::Strategies::OpenIDConnect::ALLOWED_IDP_HOSTS_DISCOVERY_URL_MAP',
               { idp_host => "https://#{idp_host}/.well-known/openid-configuration" })
  end

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
    let(:jwk_set) { JSON::JWK::Set.new({ 'keys' => [JSON.parse(JSON::JWK.new(rsa_key.public_key).to_json)] }) }

    before do
      allow(strategy).to receive(:provider_jwk_set).and_return(jwk_set)
    end

    it 'returns the payload when the signature matches the published key' do
      result = strategy.send(:decode_verified_jwt, signed_jwt(payload))
      expect(result).to eq(payload)
    end

    it 'raises when the JWT is signed by a different key than the one published' do
      token = signed_jwt(payload, key: OpenSSL::PKey::RSA.generate(2048))
      expect { strategy.send(:decode_verified_jwt, token) }.to raise_error(JSON::JWT::VerificationFailed)
    end

    it 'raises when the JWT header kid does not match any published key' do
      token = signed_jwt(payload, kid: 'unknown-kid')
      expect { strategy.send(:decode_verified_jwt, token) }.to raise_error(JSON::JWK::Set::KidNotFound)
    end
  end

  describe '#provider_jwk_set' do
    let(:well_known_uri) { "https://#{idp_host}/.well-known/openid-configuration" }
    let(:jwks_uri) { "https://#{idp_host}/jwks" }
    let(:jwks_document) { { 'keys' => [JSON.parse(JSON::JWK.new(rsa_key.public_key).to_json)] } }

    before do
      stub_request(:get, well_known_uri).to_return(
        body: { 'jwks_uri' => jwks_uri }.to_json, headers: { 'Content-Type' => 'application/json' }
      )
      stub_request(:get, jwks_uri).to_return(
        body: jwks_document.to_json, headers: { 'Content-Type' => 'application/json' }
      )
    end

    it 'raises when the client host is not one of the identity providers configured in csp.yml' do
      stub_const('OmniAuth::Strategies::OpenIDConnect::ALLOWED_IDP_HOSTS_DISCOVERY_URL_MAP',
                 { 'other-idp.example.com' => 'https://other-idp.example.com/.well-known/openid-configuration' })
      expect { strategy.send(:provider_jwk_set) }.to raise_error(ArgumentError, /unlisted identity provider host/)
    end

    it 'discovers the jwks_uri via the well-known configuration endpoint and builds a matching key set' do
      jwk_set = strategy.send(:provider_jwk_set)
      expect(jwk_set[jwk[:kid]]).to be_present
    end

    it 'caches the discovered key set instead of refetching on every call' do
      allow(Rails).to receive(:cache).and_return(ActiveSupport::Cache::MemoryStore.new)

      strategy.send(:provider_jwk_set)
      strategy.send(:provider_jwk_set)

      expect(a_request(:get, well_known_uri)).to have_been_made.once
      expect(a_request(:get, jwks_uri)).to have_been_made.once
      # expect(OpenIDConnect.http_client).to have_received(:get).with(well_known_uri).once
      # expect(OpenIDConnect.http_client).to have_received(:get).with(jwks_uri).once
    end
  end

  describe '#well_known_configuration_uri' do
    it 'uses the discovery endpoint as configured in csp.yml file' do
      expect(strategy.send(:well_known_configuration_uri))
        .to eq("https://#{idp_host}/.well-known/openid-configuration")
    end
    it 'builds the discovery endpoint from the client host, default scheme/port' \
       'and the default path for well known configuration' do
      expect(strategy.send(:well_known_configuration_uri))
        .to eq("https://#{idp_host}/.well-known/openid-configuration")
    end

    it 'includes a non-default port' do
      strategy = described_class.new(nil, client_options: { host: idp_host, port: 8443 })
      stub_const('OmniAuth::Strategies::OpenIDConnect::ALLOWED_IDP_HOSTS_DISCOVERY_URL_MAP',
                 { idp_host => nil })
      expect(strategy.send(:well_known_configuration_uri))
        .to eq("https://#{idp_host}:8443/.well-known/openid-configuration")
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
    let(:well_known_uri) { "https://#{idp_host}/.well-known/openid-configuration" }
    let(:jwks_uri) { "https://#{idp_host}/jwks" }
    let(:jwks_document) { { 'keys' => [JSON.parse(JSON::JWK.new(rsa_key.public_key).to_json)] } }

    before do
      allow(strategy).to receive(:access_token).and_return(double(access_token: 'a-token'))
      stub_request(:get, userinfo_uri)
        .with(headers: { 'Authorization' => 'Bearer a-token' })
        .to_return(body: signed_jwt(payload), headers: { 'Content-Type' => 'application/jwt' })
      stub_request(:get, well_known_uri).to_return(
        body: { 'jwks_uri' => jwks_uri }.to_json, headers: { 'Content-Type' => 'application/json' }
      )
      stub_request(:get, jwks_uri).to_return(
        body: jwks_document.to_json, headers: { 'Content-Type' => 'application/json' }
      )
    end

    it 'verifies the signature and returns the userinfo claims' do
      result = strategy.send(:fetch_userinfo_payload)
      expect(result).to include('sub' => '12345', 'email' => 'test@example.com')
    end

    it 'raises when the userinfo JWT is signed by an untrusted key' do
      tampered_jwt = signed_jwt(payload, key: OpenSSL::PKey::RSA.generate(2048))
      stub_request(:get, userinfo_uri)
        .with(headers: { 'Authorization' => 'Bearer a-token' })
        .to_return(status: 200, body: tampered_jwt, headers: { 'Content-Type' => 'application/jwt' })

      expect { strategy.send(:fetch_userinfo_payload) }.to raise_error(JSON::JWT::VerificationFailed)
    end
  end
end
