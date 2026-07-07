# frozen_string_literal: true

require 'securerandom'

module LoginSupport
  def create_user_with_csp(given_name: 'John', family_name: 'Smith', csp: :login_dot_gov,
                           uuid: SecureRandom.uuid, **user_attrs)
    csp = Csp.find_by(name: csp.to_s) || create(:csp, csp)
    user = create(:user, given_name:, family_name:, **user_attrs)
    create(:csp_user, user:, uuid:, csp:)
    user
  end

  def create_user_and_sign_in(given_name: 'John', family_name: 'Smith', csp: :login_dot_gov, uuid: SecureRandom.uuid)
    user = create_user_with_csp(given_name:, family_name:, csp:, uuid:)
    sign_in user, csp:
    user
  end

  def sign_in(user, csp:)
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

  def fetch_csp_user!(user, csp_name)
    user.csp_user_for(csp_name.to_s) ||
      raise(ArgumentError, "No csp_user found for user ##{user.id} with CSP '#{csp_name}'")
  end

  def login_dot_gov_auth_hash(user)
    csp_user   = fetch_csp_user!(user, 'login_dot_gov')
    all_emails = csp_user.user_emails.map(&:email)
    primary_email = all_emails.first

    { uid: csp_user.uuid,
      info: { email: primary_email },
      credentials: { token: 'mock_token', expires_in: 899 },
      extra: {
        raw_info: {
          given_name: user.given_name,
          family_name: user.family_name,
          all_emails:,
          ial: 'http://idmanagement.gov/ns/assurance/ial/2'
        }
      } }
  end

  def id_me_auth_hash(user)
    csp_user   = fetch_csp_user!(user, 'id_me')
    all_emails = csp_user.user_emails.map(&:email)
    primary_email = all_emails.first

    { uid: csp_user.uuid,
      info: { email: primary_email },
      credentials: { token: 'mock_token', expires_in: 300 },
      extra: {
        raw_info: {
          SSN: 111_887_777,
          identity_assurance_level: 2,
          emails_confirmed: all_emails,
          email: user.email
        }
      } }
  end

  def clear_auth_hash(user)
    csp_user   = fetch_csp_user!(user, 'login_dot_gov')
    all_emails = csp_user.user_emails.map(&:email)
    primary_email = all_emails.first

    { uid: csp_user.uuid,
      info: { email: primary_email },
      extra: {
        raw_info: {
          all_emails: all_emails,
          ial: 'http://idmanagement.gov/ns/assurance/ial/1'
        }
      } }
  end
end
