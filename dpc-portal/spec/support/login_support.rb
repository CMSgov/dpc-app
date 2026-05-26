# frozen_string_literal: true

require 'securerandom'

module LoginSupport
  def sign_in(user, csp: :id_me)
    OmniAuth.config.test_mode = true
    case csp.to_s
    when 'id_me'
      OmniAuth.config.add_mock(:id_me, id_me_auth_hash(user))
    when 'login_dot_gov'
      OmniAuth.config.add_mock(:login_dot_gov, login_dot_gov_auth_hash(user))
    when 'clear'
      OmniAuth.config.add_mock(:clear, clear_auth_hash(user))
    else raise ArgumentError, "Unknown CSP code: #{csp}"
    end
    post "/auth/#{csp}"
    follow_redirect!
  end

  def login_dot_gov_auth_hash(user)
    { uid: user.uid,
      info: { email: user.email },
      # credentials: { token: 'mock_token', expires_in: 300 },
      extra: {
        raw_info: {
          all_emails: [user.email],
          ial: 'http://idmanagement.gov/ns/assurance/ial/1'
        }
      } }
  end

  def id_me_auth_hash(user)
    { uid: user.uid,
      info: { email: user.email },
      # credentials: { token: 'mock_token', expires_in: 300 },
      extra: {
        raw_info: {
          SSN: 111_887_777,
          identity_assurance_level: 1,
          emails_confirmed: [user.email],
          email: user.email
        }
      } }
  end

  def clear_auth_hash(user)
    { uid: user.uid,
      info: { email: user.email },
      extra: {
        raw_info: {
          all_emails: [user.email],
          ial: 'http://idmanagement.gov/ns/assurance/ial/1'
        }
      } }
  end
end
