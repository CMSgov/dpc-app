# frozen_string_literal: true

# A service that verifies generates an ao invitation
class UserInfoService
  USER_INFO_URI = URI('https://idp.int.identitysandbox.gov/api/openid_connect/userinfo')

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
    response = Net::HTTP.get_response(USER_INFO_URI, auth_header(token))
    case response.code.to_i
    when 200...299
      parsed_response(response)
    when 401
      raise UserInfoServiceError, 'unauthorized'
    else
      Rails.logger.error "User Info Error: #{response.body}"
      raise UserInfoServiceError, 'server_error'
    end
  rescue Errno::ECONNREFUSED
    connection_error
  end

  def parsed_response(response)
    return if response.body.blank?

    JSON.parse response.body
  end

  def connection_error
    Rails.logger.error 'Could not connect to login.gov'
    raise UserInfoServiceError, 'connection_error'
  end
end

class UserInfoServiceError < StandardError; end
