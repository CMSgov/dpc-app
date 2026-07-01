# frozen_string_literal: true

# A service that verifies generates an ao invitation
class UserInfoService
  USER_INFO_URI = URI("https://#{ENV.fetch('IDP_HOST')}/api/openid_connect/userinfo")

  def user_info(session)
    validate_session(session)

    request_info(session[:login_dot_gov_token])
  end

  private

  def auth_header(token)
    { Authorization: "Bearer #{token}" }
  end

  def validate_session(session)
    raise UserInfoServiceError, 'no_token' unless session[:login_dot_gov_token].present?
    raise UserInfoServiceError, 'no_token_exp' unless session[:login_dot_gov_token_exp].present?
    raise UserInfoServiceError, 'expired_token' unless session[:login_dot_gov_token_exp] > Time.now
  end

  def request_info(token)
    start_tracking

    response = trace_request do
      Net::HTTP.get_response(USER_INFO_URI, auth_header(token))
    end

    handle_response(response)
  rescue Errno::ECONNREFUSED
    @error_code = 503
    Rails.logger.error 'Could not connect to login.gov'
    raise UserInfoServiceError, 'server_error'
  ensure
    finish_tracking(@error_code || response&.code)
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

  def parsed_response(response)
    return if response.body.blank?

    JSON.parse response.body
  end

  def start_tracking
    @start = Time.now
    Rails.logger.info(
      ['Calling Login.gov user_info',
       { login_dot_gov_request_method: :get,
         login_dot_gov_request_url: USER_INFO_URI,
         login_dot_gov_request_method_name: :request_info }]
    )
  end

  def finish_tracking(code)
    Rails.logger.info(
      ['Login.gov user_info response info',
       { login_dot_gov_request_method: :get,
         login_dot_gov_request_url: USER_INFO_URI,
         login_dot_gov_request_method_name: :request_info,
         login_dot_gov_response_status_code: code&.to_i,
         login_dot_gov_response_duration: Time.now - @start }]
    )
  end
end

class UserInfoServiceError < StandardError; end
