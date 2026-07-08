# frozen_string_literal: true

# A service that verifies generates an ao invitation
class UserInfoService
  def user_info(csp_session)
    validate_session(csp_session)

    request_info(csp_session.current, csp_session.token)
  end

  private

  def auth_header(token)
    { Authorization: "Bearer #{token}" }
  end

  def validate_session(csp_session)
    reason = csp_session.inactive_reason
    raise UserInfoServiceError, reason.to_s if reason
  end

  def oidc_client_config(csp)
    return ID_ME_CLIENT_CONFIG if csp.to_s == :id_me.to_s
    return LOGIN_DOT_GOV_CLIENT_CONFIG if csp.to_s == :login_dot_gov.to_s

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

  def request_info(csp, token)
    csp_config = oidc_client_config csp
    user_info_uri = csp_config[:client_options][:userinfo_endpoint]
    start_tracking csp, user_info_uri

    response = trace_request(user_info_uri) do
      Net::HTTP.get_response(URI(user_info_uri), auth_header(token))
    end

    code = response.code
    handle_response(response)
  rescue Errno::ECONNREFUSED
    code = 503
    Rails.logger.error 'Could not connect to login.gov'
    raise UserInfoServiceError, 'server_error'
  ensure
    finish_tracking(code, csp, user_info_uri)
  end

  def trace_request(user_info_uri)
    Datadog::Tracing.trace('user_info_service.request', resource: 'request_info') do |span|
      span.type = 'http'
      span.set_tag('http.url', user_info_uri)
      span.set_tag('http.method', 'GET')

      raw_response = yield

      span.set_tag('http.status_code', raw_response.code)
      raw_response
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
