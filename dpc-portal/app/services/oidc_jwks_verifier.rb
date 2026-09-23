# frozen_string_literal: true

require 'json/jwt'
require 'openid_connect'

# Verifies a userinfo JWT's signature against the JWK Set published by the
# issuing identity provider.
# Utilizes csp.yml as the source of truth for
# a) list of hosts for legit csps
# b) Discovery document path for each host, which is used to fetch the jwks_uri
# c) Any explicit jwks_uri for each host, which is used to validate the jwks uri
#    as published in the discovery document.
class OidcJwksVerifier
  class UntrustedJwksUriError < StandardError; end

  CSP_IDP_CONFIGS = Rails.application.config_for(:csp).values.grep(Hash).freeze

  # Host -> discovery document path as configured in config/csp.yml.
  ALLOWED_IDP_HOSTS_DISCOVERY_URL_MAP = CSP_IDP_CONFIGS
                                        .filter_map { |c| [c[:host], c[:discovery_uri]] if c[:host].present? }
                                        .to_h.freeze

  # Set of legit JWKS host: each provider's own host, plus any host it
  # explicitly publishes as its jwks_uri in csp.yml.
  # A discovery document's jwks_uri is trusted only when it resolves to one of
  # these hosts.
  ALLOWED_JWKS_HOSTS = (
    CSP_IDP_CONFIGS.filter_map { |c| c[:host] } +
    CSP_IDP_CONFIGS.filter_map do |c|
      next if c[:jwks_uri].blank?

      URI.parse(c[:jwks_uri]).host
    rescue URI::InvalidURIError
      nil
    end
  ).to_set.freeze

  class << self
    # Decodes +jwt_string+, verifying its signature against +host+'s
    # published JWKS. If the JWT's kid isn't found in our cached key set,
    # the JWKS is refreshed once before giving up, so that a key rotated in
    # by the provider but missing in the stale JWKS doesn't cause failures
    def decode_and_verify(jwt_string, host:)
      JSON::JWT.decode(jwt_string, jwk_set_for(host)).to_h
    rescue JSON::JWK::Set::KidNotFound
      JSON::JWT.decode(jwt_string, jwk_set_for(host, force_refresh: true)).to_h
    end

    private

    def jwk_set_for(host, force_refresh: false)
      unless ALLOWED_IDP_HOSTS_DISCOVERY_URL_MAP.key?(host)
        raise ArgumentError, "Refusing to fetch OIDC keys for unlisted identity provider host: #{host}"
      end

      cache_key = "oidc_jwks/#{host}"
      Rails.cache.delete(cache_key) if force_refresh

      jwks = Rails.cache.fetch(cache_key, expires_in: 1.hour) do
        well_known_config = fetch_json(well_known_uri(host))
        fetch_jwks(well_known_config.fetch('jwks_uri'))
      end
      JSON::JWK::Set.new(jwks)
    end

    def well_known_uri(host)
      "https://#{host}#{ALLOWED_IDP_HOSTS_DISCOVERY_URL_MAP.fetch(host)}"
    end

    def fetch_jwks(jwks_uri)
      uri = URI.parse(jwks_uri)
      unless uri.is_a?(URI::HTTPS) && ALLOWED_JWKS_HOSTS.include?(uri.host)
        raise UntrustedJwksUriError, "Refusing to fetch JWKS from untrusted URI: #{jwks_uri}"
      end

      fetch_json(uri)
    end

    def fetch_json(uri)
      response = ::OpenIDConnect.http_client.get(uri)
      return response.body if response.body.is_a?(Hash)

      JSON.parse(response.body.to_s)
    end
  end
end
