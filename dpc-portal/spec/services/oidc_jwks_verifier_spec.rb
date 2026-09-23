# frozen_string_literal: true

require 'rails_helper'

describe OidcJwksVerifier do
  let(:idp_host) { 'idp.example.com' }
  let(:well_known_uri) { "https://#{idp_host}/.well-known/openid-configuration" }
  let(:jwks_uri) { "https://#{idp_host}/jwks" }
  let(:payload) { { 'sub' => '12345', 'email' => 'test@example.com' } }
  let(:rsa_key) { OpenSSL::PKey::RSA.generate(2048) }
  let(:jwk) { JSON::JWK.new(rsa_key) }

  def signed_jwt(claims, key: rsa_key, kid: jwk[:kid])
    token = JSON::JWT.new(claims)
    token.kid = kid
    token.sign(key, :RS256).to_s
  end

  def stub_discovery(jwks_uri:)
    stub_request(:get, well_known_uri).to_return(
      body: { 'jwks_uri' => jwks_uri }.to_json, headers: { 'Content-Type' => 'application/json' }
    )
  end

  def stub_jwks(uri, key: rsa_key)
    stub_request(:get, uri).to_return(
      body: { 'keys' => [JSON.parse(JSON::JWK.new(key.public_key).to_json)] }.to_json,
      headers: { 'Content-Type' => 'application/json' }
    )
  end

  before do
    stub_const('OidcJwksVerifier::ALLOWED_IDP_HOSTS_DISCOVERY_URL_MAP',
               { idp_host => '/.well-known/openid-configuration' })
    stub_const('OidcJwksVerifier::ALLOWED_JWKS_HOSTS', Set.new([idp_host]))
  end

  describe '.decode_and_verify' do
    it 'raises when the host is not an allowlisted identity provider' do
      expect { described_class.decode_and_verify(signed_jwt(payload), host: 'other.example.com') }
        .to raise_error(ArgumentError, /unlisted identity provider host/)
    end

    context 'when the host is allowlisted' do
      before do
        stub_discovery(jwks_uri: jwks_uri)
        stub_jwks(jwks_uri)
      end

      it 'returns the payload when the signature matches the published key' do
        result = described_class.decode_and_verify(signed_jwt(payload), host: idp_host)
        expect(result).to eq(payload)
      end

      it 'raises when the JWT is signed by a different key than the one published' do
        token = signed_jwt(payload, key: OpenSSL::PKey::RSA.generate(2048))
        expect { described_class.decode_and_verify(token, host: idp_host) }
          .to raise_error(JSON::JWT::VerificationFailed)
      end
    end

    context 'when the discovered jwks_uri points outside the allowlisted JWKS hosts' do
      let(:malicious_uri) { 'https://attacker.example.com/jwks' }

      before do
        stub_discovery(jwks_uri: malicious_uri)
      end

      it 'refuses to fetch it and never issues the request' do
        expect { described_class.decode_and_verify(signed_jwt(payload), host: idp_host) }
          .to raise_error(described_class::UntrustedJwksUriError, /untrusted URI/)
        expect(a_request(:get, malicious_uri)).not_to have_been_made
      end
    end

    context 'when the discovered jwks_uri is not https' do
      let(:insecure_uri) { "http://#{idp_host}/jwks" }

      before do
        stub_discovery(jwks_uri: insecure_uri)
      end

      it 'refuses to fetch it' do
        expect { described_class.decode_and_verify(signed_jwt(payload), host: idp_host) }
          .to raise_error(described_class::UntrustedJwksUriError)
        expect(a_request(:get, insecure_uri)).not_to have_been_made
      end
    end

    context 'caching' do
      before do
        allow(Rails).to receive(:cache).and_return(ActiveSupport::Cache::MemoryStore.new)
        stub_discovery(jwks_uri: jwks_uri)
        stub_jwks(jwks_uri)
      end

      it 'caches the discovered key set instead of refetching on every call' do
        described_class.decode_and_verify(signed_jwt(payload), host: idp_host)
        described_class.decode_and_verify(signed_jwt(payload), host: idp_host)

        expect(a_request(:get, well_known_uri)).to have_been_made.once
        expect(a_request(:get, jwks_uri)).to have_been_made.once
      end
    end

    context 'when the cached key set predates a key rotation (kid miss)' do
      let(:rotated_key) { OpenSSL::PKey::RSA.generate(2048) }
      let(:rotated_jwk) { JSON::JWK.new(rotated_key) }

      before do
        allow(Rails).to receive(:cache).and_return(ActiveSupport::Cache::MemoryStore.new)
        stub_discovery(jwks_uri: jwks_uri)
        stub_jwks(jwks_uri, key: rsa_key)

        # Prime the cache with the pre-rotation key set.
        described_class.decode_and_verify(signed_jwt(payload), host: idp_host)

        # The provider rotates its signing key; the cache is now stale.
        stub_jwks(jwks_uri, key: rotated_key)
      end

      it 'refreshes the JWKS once and successfully verifies a JWT signed by the new key' do
        token = signed_jwt(payload, key: rotated_key, kid: rotated_jwk[:kid])

        result = described_class.decode_and_verify(token, host: idp_host)

        expect(result).to eq(payload)
        expect(a_request(:get, jwks_uri)).to have_been_made.twice
      end

      it 'raises KidNotFound after a single refresh attempt when the kid is still unknown' do
        token = signed_jwt(payload, kid: 'still-unknown')

        expect { described_class.decode_and_verify(token, host: idp_host) }
          .to raise_error(JSON::JWK::Set::KidNotFound)
        expect(a_request(:get, jwks_uri)).to have_been_made.twice
      end
    end
  end
end
