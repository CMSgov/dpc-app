# frozen_string_literal: true

RSpec.shared_examples 'a CSP client' do |provider, auth_endpoint|
  csp_name = provider.to_s

  context 'user exists' do
    before do
      user = create(:user, email: 'bob1@example.com', provider:)
      create(:csp_user, user:, uuid:, csp:)
    end

    it 'should sign in a user' do
      post auth_endpoint
      follow_redirect!
      expect(response.location).to eq organizations_url
      expect(response).to be_redirect
      follow_redirect!
      expect(response).to be_ok
    end

    it 'should log on successful sign in' do
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(['User logged in',
                                                   { actionContext: LoggingConstants::ActionContext::Authentication,
                                                     actionType: LoggingConstants::ActionType::UserLoggedIn,
                                                     csp: csp_name }])
      post auth_endpoint
      follow_redirect!
    end

    it 'should write a cookie with the last used csp' do
      post auth_endpoint
      follow_redirect!
      expect(cookies[:last_used_csp]).to eq csp_name
    end

    it 'should not add another user credential' do
      expect(CspUser.where(uuid:, csp:).count).to eq 1
      expect do
        post auth_endpoint
        follow_redirect!
      end.to change { CspUser.count }.by(0)
    end
  end

  context 'user does not exist' do
    it 'should not persist user' do
      expect do
        post auth_endpoint
        follow_redirect!
      end.to change { User.count }.by(0)
    end
  end
end
