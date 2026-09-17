require 'json/jwt'
require 'openid_connect'

module OmniAuth
  module Strategies
    class OpenIDConnect
      # Trusted hosts for CSPs as configured in config/csp.yml, mapped to their discovery URIs. 
      ALLOWED_IDP_HOSTS_DISCOVERY_URL_MAP = Rails.application.config_for(:csp).values
                               .select { |value| value.is_a?(Hash) }
                               .filter_map { |value| [value[:host], value[:discovery_uri]] if value[:host].present? }
                               .to_h

      def user_info
        @user_info ||= ::OpenIDConnect::ResponseObject::UserInfo.new(fetch_userinfo_payload)
      rescue => e
        Rails.logger.error(['OIDC userinfo processing failed',
                    { actionContext: LoggingConstants::ActionContext::Authentication,
                      actionType: LoggingConstants::ActionType::OidcUserInfoFailed,
                      exceptionClass: e.class.name }])
        fail!(:user_info_failed, e)
        nil
      end

      private

      # Calls the userinfo endpoint with the bearer access token and returns
      # the claims as a Hash. If the IdP responds with a signed JWT
      # (application/jwt), the JWT's signature is verified against the
      # provider's published JWKS before its payload is trusted. Otherwise
      # the JSON body is parsed.
      # Fetches and parses the userinfo payload from the OpenID Connect provider.
      #
      # This method retrieves user information from the userinfo endpoint using the access token,
      # handles various response formats (JSON, JWT, JSON-encoded JWT), and returns the parsed payload.
      #
      # The method handles several IdP variations:
      # - Some providers return raw JSON
      # - Some providers return a JWT (JSON Web Token)
      # - Some providers JSON-encode the JWT, wrapping it as a string: `"<jwt>"`
      #
      # @return [Hash] A hash with indifferent access containing the userinfo payload.
      #   If the response is a JWT, its signature is verified and the payload is converted to a hash.
      #   If the response is JSON, it is parsed and converted to a hash.
      #   Keys can be accessed with symbols or strings.
      #
      # @note JSON::JWT.decode returns a JWT object that responds to #to_h, converting it to a Hash
      def fetch_userinfo_payload
        response = ::OpenIDConnect.http_client.get(
          userinfo_endpoint_uri,
          nil,
          { 'Authorization' => "Bearer #{access_token.access_token}" }
        )

        # If already parsed into a Hash upstream, return it directly
        return response.body.with_indifferent_access if response.body.is_a?(Hash)

        body = response.body.to_s.strip
        ct_header = Array(response.headers['Content-Type']).first.to_s
        content_type = ct_header.split(';').first.to_s.strip.downcase

        if content_type == 'application/jwt' || looks_like_jwt?(body)
          body = body[1..-2] if body.start_with?('"') && body.end_with?('"')
          decode_verified_jwt(body).with_indifferent_access
        else
          JSON.parse(body).with_indifferent_access
        end
      end

      # Decodes a userinfo JWT, verifying its signature against the key
      # published by the issuing provider. The signing key set is located via
      # OpenID Connect Discovery (the provider's well-known configuration
      # endpoint), and discovery is only ever performed for a host listed as
      # an identity provider in config/csp.yml.
      def decode_verified_jwt(jwt_string)
        JSON::JWT.decode(jwt_string, provider_jwk_set).to_h
      end

      # Fetches (and caches) the JWK Set published by the current provider,
      # discovering its jwks_uri via the OIDC well-known configuration
      # endpoint or using the discovery URI configured in config/csp.yml.
      def provider_jwk_set
        host = client_options.host
        unless ALLOWED_IDP_HOSTS_DISCOVERY_URL_MAP.keys.include?(host)
          raise ArgumentError, "Refusing to fetch OIDC keys for unlisted identity provider host: #{host}"
        end

        jwks = Rails.cache.fetch("oidc_jwks/#{host}", expires_in: 1.hour) do
          well_known_config = fetch_json(well_known_configuration_uri)
          fetch_json(well_known_config.fetch('jwks_uri'))
        end
        JSON::JWK::Set.new(jwks)
      end

      def fetch_json(uri)
        response = ::OpenIDConnect.http_client.get(uri)
        return response.body if response.body.is_a?(Hash)

        JSON.parse(response.body.to_s)
      end

      def userinfo_endpoint_uri
        provider_uri(client_options.userinfo_endpoint)
      end

      def well_known_configuration_uri
        provider_uri(ALLOWED_IDP_HOSTS_DISCOVERY_URL_MAP[client_options.host] || '/.well-known/openid-configuration')
      end

      def provider_uri(endpoint)
        parsed = URI.parse(endpoint)
        return parsed.to_s if parsed.is_a?(URI::HTTP) || parsed.is_a?(URI::HTTPS)

        host_with_port =
          if client_options.port && ![80, 443].include?(client_options.port)
            "#{client_options.host}:#{client_options.port}"
          else
            client_options.host
          end
        "#{client_options.scheme}://#{host_with_port}#{endpoint}"
      end

      def looks_like_jwt?(body)
        parts = body.to_s.strip.split('.')
        parts.length == 3 && parts.all? { |p| p.match?(/\A[A-Za-z0-9_-]+\z/) }
      end
    end
  end
end
