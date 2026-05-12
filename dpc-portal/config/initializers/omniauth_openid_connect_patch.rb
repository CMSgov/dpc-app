require 'json/jwt'
require 'openid_connect'

module OmniAuth
  module Strategies
    class OpenIDConnect
      def user_info
        @user_info ||= ::OpenIDConnect::ResponseObject::UserInfo.new(fetch_userinfo_payload)
      rescue => e
        Rails.logger.error "[OIDC Patch Error] #{e.class}: #{e.message}"
        fail!(:user_info_failed, e)
        nil
      end

      private

      # Calls the userinfo endpoint with the bearer access token and returns
      # the claims as a Hash. If the IdP responds with a signed JWT
      # (application/jwt), the JWT is decoded without signature verification
      # and the payload is returned. Otherwise the JSON body is parsed.
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
      #   If the response is a JWT, it is decoded and converted to a hash.
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
        body = response.body.to_s.strip
        ct_header = Array(response.headers['Content-Type']).first.to_s
        content_type = ct_header.split(';').first.to_s.strip.downcase

        if content_type == 'application/jwt' || looks_like_jwt?(body)
          body = body[1..-2] if body.start_with?('"') && body.end_with?('"')
          ## TODO - consider verifying the JWT signature using the provider's JWKS keys
          JSON::JWT.decode(body, :skip_verification).to_h.with_indifferent_access
        else
          JSON.parse(body).with_indifferent_access
        end
      end

      def userinfo_endpoint_uri
        endpoint = client_options.userinfo_endpoint
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
