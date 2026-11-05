# frozen_string_literal: true

# Handles interactions with login.gov.
class LoginDotGovController < CspController
  def csp_code     = :login_dot_gov
  def display_name = 'Login.gov'

  private

  def additional_verification_blocked?
    return false if facial_match_verified?(auth_details)

    render_facial_match_blocked
    true
  end

  def facial_match_verified?(auth)
    ial = auth.extra.raw_info.ial
    ial.present? && ial.to_s == CspIdentityAssurance::FACIAL_MATCH_REQUIRED
  end

  def render_facial_match_blocked
    log_event(:error, "User verified with #{display_name} but without required facial match",
              action_context: LoggingConstants::ActionContext::Authentication,
              action_type: LoggingConstants::ActionType::UserLoginWithoutAccount,
              user_identifier: auth_details&.uid,
              csp: auth_details&.provider)
    render(Page::Utility::ErrorComponent.new(nil, 'csp_signin_fail', csp: csp_code), status: :forbidden)
  end
end
