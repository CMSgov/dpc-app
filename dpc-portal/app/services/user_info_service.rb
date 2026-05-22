# frozen_string_literal: true

# A service that verifies generates an ao invitation
class UserInfoService
  def user_info(session)
    validate_session(session)

    # request_info(session[:login_dot_gov_token])
    request_info(session[:csp], session["#{session[:csp]}_token"])
  end

  private

  def auth_header(token)
    { Authorization: "Bearer #{token}" }
  end

  def validate_session(session)
    raise UserInfoServiceError, 'no_session' unless session[:csp].present?

    csp = session[:csp]
    raise UserInfoServiceError, 'no_token' unless session["#{csp}_token"].present?
    raise UserInfoServiceError, 'no_token_exp' unless session["#{csp}_token_exp"].present?
    raise UserInfoServiceError, 'expired_token' unless session["#{csp}_token_exp"] > Time.now
  end

  def parsed_response(response)
    return if response.body.blank?

    body = response.body.to_s.strip
    if response.content_type.to_s.strip.downcase == 'application/jwt' || looks_like_jwt?(body)
      decode_jwt(body)
    else
      JSON.parse(body).with_indifferent_access
    end
  end

  def looks_like_jwt?(body)
    parts = body.to_s.strip.split('.')
    parts.length == 3 && parts.all? { |p| p.match?(/\A[A-Za-z0-9_-]+\z/) }
  end

  def decode_jwt(body)
    body = body[1..-2] if body.start_with?('"') && body.end_with?('"')
    JSON::JWT.decode(body, :skip_verification).to_h.with_indifferent_access
  end

  def oidc_client_config(csp)
    return ID_ME_CLIENT_CONFIG if csp.to_s == :id_me.to_s
    return LOGIN_DOT_GOV_CLIENT_CONFIG if csp.to_s == :login_dot_gov.to_s

    # TODO: Add CLEAR_CONFIG here
    USER_INFO_URI = URI("https://#{ENV.fetch('CLEAR_IDP_HOST')}/integrations/userinfo")
    USER_INFO_CLAIMS = {
      id_token: {
        ssn9: nil,
        email: nil,
        email_verified: nil
      },
      userinfo: {
        ssn9: nil,
        email: nil,
        email_verified: nil,
        given_name: nil,
        family_name: nil
      }
    }.to_json
    USER_INFO_CLAIMS_URI = URI("#{USER_INFO_URI}?#{ { claims: USER_INFO_CLAIMS }.to_query }")

    # TODO move to initializers
    clear_idp_host = ENV['CLEAR_IDP_HOST']
    # move "client_id" to "identifier" for CLEAR_CONFIG
    # clear_client_id = ENV['CLEAR_IDP_CLIENT_ID']
    clear_issuer = "https://#{clear_idp_host}/integrations"
    CLEAR_CLIENT_CONFIG = {
      name: :clear,
      issuer: clear_issuer,
      # discovery: false,
      scope: "openid",
      response_type: :code,
      # acr_values: 'http://idmanagement.gov/ns/assurance/ial/1',
      # client_auth_method: :jwt_bearer,
      client_auth_method: :client_secret_post,
      client_options: {
        port: 443,
        scheme: 'https',
        host: "https://#{clear_idp_host}/",
        identifier: "urn:gov:cms:openidconnect.profiles:sp:sso:cms:dpc:#{ENV['ENV']}",
        # private_key: ENV['LOGIN_DOT_GOV_CLIENT_PRIVATE_KEY'],
        secret: ENV['CLEAR_IDP_CLIENT_SECRET'],
        redirect_uri: "#{my_protocol_host}/auth/clear/callback",

        authorization_endpoint: "#{clear_issuer}/oauth2/auth",
        token_endpoint: "#{clear_issuer}/oauth2/token",
        userinfo_endpoint: USER_INFO_CLAIMS_URI.to_s,
        jwks_uri: "#{clear_issuer}/.well-known/jwks.json"
      }
    }
    return CLEAR_CLIENT_CONFIG if csp.to_s == :clear.to_s

    raise UserInfoServiceError, 'invalid_csp'
  end

  def request_info(csp, token) # rubocop:disable Metrics/AbcSize
    csp_config = oidc_client_config csp
    start_tracking csp, csp_config[:client_options][:userinfo_endpoint]
    response = Net::HTTP.get_response(URI(csp_config[:client_options][:userinfo_endpoint]), auth_header(token))
    code = response.code.to_i
    case code
    when 200...299
      user_info = parsed_response(response)
      Rails.logger.info(['CLEAR userinfo response',
                         { sub: user_info&.dig('sub'),
                           email: user_info&.dig('email'),
                           email_verified: user_info&.dig('email_verified'),
                           given_name_present: user_info&.dig('given_name').present?,
                           family_name_present: user_info&.dig('family_name').present?,
                           ssn9_present: user_info&.dig('ssn9').present?,
                           social_security_number_present: user_info&.dig('social_security_number').present? }])
      user_info
    when 401
      raise UserInfoServiceError, 'unauthorized'
    else
      Rails.logger.error "User Info Error: #{response.body}"
      raise UserInfoServiceError, 'server_error'
    end
  rescue Errno::ECONNREFUSED
    code = 503
    Rails.logger.error 'Could not connect to login.gov'
    raise UserInfoServiceError, 'server_error'
  ensure
    finish_tracking(code, csp, csp_config[:client_options][:userinfo_endpoint])
  end

  def start_tracking(csp, user_info_uri)
    @start = Time.now
    Rails.logger.info(
      ['Calling CSP user_info',
       { csp: csp,
         csp_request_method: :get,
         csp_request_url: user_info_uri,
         csp_request_method_name: :request_info }]
    )
    @tracker = NewRelic::Agent::Tracer.start_external_request_segment(library: 'Net::HTTP', uri: user_info_uri,
                                                                      procedure: :get)
  end

  def finish_tracking(code, csp, user_info_uri)
    @tracker.finish
    Rails.logger.info(
      ['CSP user_info response info',
       { csp: csp,
         csp_request_method: :get,
         csp_request_url: user_info_uri,
         csp_request_method_name: :request_info,
         csp_response_status_code: code,
         csp_response_duration: Time.now - @start }]
    )
  end
end

class UserInfoServiceError < StandardError; end
