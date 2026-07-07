# frozen_string_literal: true

# A service that verifies generates an ao invitation
class UserInfoService
  def user_info(session)
    validate_session(session)

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

  def oidc_client_config(csp)
    return ID_ME_CLIENT_CONFIG if csp.to_s == :id_me.to_s
    return LOGIN_DOT_GOV_CLIENT_CONFIG if csp.to_s == :login_dot_gov.to_s
    return CLEAR_CLIENT_CONFIG if csp.to_s == :clear.to_s

    raise UnknownCSPError, csp
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

  def request_info(csp, token) # rubocop:disable Metrics/AbcSize
    csp_config = oidc_client_config csp
    start_tracking csp, csp_config[:client_options][:userinfo_endpoint]

    response = trace_request do
      Net::HTTP.get_response(URI(csp_config[:client_options][:userinfo_endpoint]), auth_header(token))
    end

    handle_response(response)
  rescue Errno::ECONNREFUSED
    code = 503
    Rails.logger.error 'Could not connect to login.gov'
    raise UserInfoServiceError, 'server_error'
  ensure
    finish_tracking(code, csp, csp_config[:client_options][:userinfo_endpoint])
  end

  def trace_request
    Datadog::Tracing.trace('user_info_service.request', resource: 'request_info') do |span|
      span.type = 'http'
      span.set_tag('http.url', USER_INFO_URI)
      span.set_tag('http.method', 'GET')

      raw_response = yield

      span.set_tag('http.status_code', raw_response.code)
      raw_response
    end
  end

  def handle_response(response)
    case response
    when Net::HTTPSuccess
      parsed_response(response)
    when Net::HTTPUnauthorized
      raise UserInfoServiceError, 'unauthorized'
    else
      Rails.logger.error "User Info Error: #{response.body}"
      raise UserInfoServiceError, 'server_error'
    end
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
  end

  def finish_tracking(code, csp, user_info_uri)
    Rails.logger.info(
      ['CSP user_info response info',
       { csp: csp,
         csp_request_method: :get,
         csp_request_url: user_info_uri,
         csp_request_method_name: :request_info,
         csp_response_status_code: code&.to_i,
         csp_response_duration: Time.now - @start }]
    )
  end
end

class UserInfoServiceError < StandardError; end
