# frozen_string_literal: true

module InvitationSupport
  def log_in(provider:, template: user_info_template)
    OmniAuth.config.test_mode = true
    OmniAuth.config.add_mock(provider.to_sym,
                             { uid: template['sub'],
                               credentials: { expires_in: 899,
                                              token: 'bearer-token' },
                               info: { email: template['email'] },
                               extra: { raw_info: { given_name: template['given_name'],
                                                    family_name: template['family_name'],
                                                    identity_assurance_level: 2 } } })
    post "/auth/#{provider}"
    follow_redirect!
  end

  def user_info_template(overrides = {})
    {
      'sub' => '097d06f7-e9ad-4327-8db3-0ba193b7a2c2',
      'iss' => 'https://api.idmelabs.com/oidc',
      'email' => 'bob@testy.com',
      'email_verified' => true,
      'all_emails' => %w[bob@testy.com david@example.com david2@example.com],
      'given_name' => 'Bob',
      'family_name' => 'Hodges',
      'birthdate' => '1938-10-06',
      'social_security_number' => '900888888',
      'verified_at' => 1_704_834_157,
      'ial' => 'http://idmanagement.gov/ns/assurance/ial/2',
      'aal' => 'urn:gov:gsa:ac:classes:sp:PasswordProtectedTransport:duo'
    }.merge(overrides)
  end

  def stub_user_info(overrides: {})
    user_service_class = class_double(UserInfoService).as_stubbed_const
    user_service = double(UserInfoService)
    allow(user_service_class).to receive(:new).and_return(user_service)

    allow(user_service).to receive(:user_info).and_return(user_info_template(overrides))
  end

  def create_invitation_user_with_csp(csp)
    template = user_info_template
    create_user_with_csp(csp:, given_name: template['given_name'], family_name: template['family_name'],
                         email: template['email'],
                         uuid: template['sub'])
  end
end
