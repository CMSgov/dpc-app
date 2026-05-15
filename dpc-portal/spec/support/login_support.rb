# frozen_string_literal: true

require 'securerandom'

module LoginSupport
  def sign_in(user)
    defaults(user)

    csp = create(:csp, name: user.provider)
    csp_user = create(:csp_user, user:, csp:, uuid: user.uid)
    OmniAuth.config.test_mode = true
    OmniAuth.config.add_mock(csp.name,
                             { uid: csp_user.uuid,
                               info: { email: user.email },
                               extra: { raw_info: { all_emails: [user.email],
                                                    ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
    post '/auth/id_me'
    follow_redirect!
  end

  private

  # Sets default values required for auth if not already set.
  def defaults(user)
    user.uid = SecureRandom.uuid if user.uid.nil?
    user.provider = :id_me if user.provider.nil?
  end
end
