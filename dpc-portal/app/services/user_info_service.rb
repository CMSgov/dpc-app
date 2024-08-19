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
    start_time = Time.now
    log_start
    response = Net::HTTP.get_response(USER_INFO_URI, auth_header(token))
    code = response.code.to_i
    case code
    when 200...299
      parsed_response(response)
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
    log_end(start_time, code)
  end

  def parsed_response(response)
    return if response.body.blank?

    JSON.parse response.body
  end

  def log_start
    Rails.logger.info(
      ['Calling Login.gov user_info',
       { login_dot_gov_request_method: :get,
         login_dot_gov_request_url: USER_INFO_URI,
         login_dot_gov_request_method_name: :request_info }]
    )
  end

  def log_end(start, code)
    Rails.logger.info(
      ['Login.gov user_info response info',
       { login_dot_gov_request_method: :get,
         login_dot_gov_request_url: USER_INFO_URI,
         login_dot_gov_request_method_name: :request_info,
         login_dot_gov_response_status_code: code,
         login_dot_gov_response_duration: Time.now - start }]
    )
  end
end

class UserInfoServiceError < StandardError; end
