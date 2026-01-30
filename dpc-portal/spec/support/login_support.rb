# frozen_string_literal: true

module LoginSupport
  def sign_in(user)
    user_credential = create(:user_credential, user:, provider: :login_dot_gov)
    OmniAuth.config.test_mode = true
    OmniAuth.config.add_mock(user_credential.provider,
                             { uid: user_credential.uid,
                               info: { email: user.email },
                               extra: { raw_info: { all_emails: [user.email],
                                                    ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
    post '/auth/login_dot_gov'
    follow_redirect!
  end
end
