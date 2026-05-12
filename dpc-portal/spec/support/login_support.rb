# frozen_string_literal: true

module LoginSupport
  def sign_in(user)
    OmniAuth.config.test_mode = true
    # OmniAuth.config.add_mock(:id_me,
    OmniAuth.config.add_mock(:clear,
                             { uid: user.uid,
                               info: { email: user.email },
                               extra: { raw_info: { all_emails: [user.email],
                                                    ial: 'http://idmanagement.gov/ns/assurance/ial/1' } } })
    # post '/auth/id_me'
    post '/auth/clear'
    follow_redirect!
  end
end
