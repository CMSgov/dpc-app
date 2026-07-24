# frozen_string_literal: true

RSpec.shared_examples 'a CSP client' do |provider, auth_endpoint, expected_id_token: nil|
  csp_name = provider.to_s

  context 'IAL/2' do
    before do
      OmniAuth.config.test_mode = true
      OmniAuth.config.add_mock(provider, csp_auth_response)
    end

    context 'user exists' do
      before do
        user = create(:user)
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

    context :user_exists do
      let(:db_user) { create(:user) }

      before do
        create(:csp_user, user: db_user, uuid:, csp:)
      end

      it 'updates user names' do
        expect do
          post auth_endpoint
          follow_redirect!
        end.to change {
          User.where(id: db_user.id, given_name: 'Bob', family_name: 'Hoskins').count
        }.by 1
        expect(response.location).to eq organizations_url
      end

      it 'sets authentication token' do
        post auth_endpoint
        follow_redirect!

        csp_session = CspSession.new(request.session)
        expect(csp_session.current).to eq csp_name
        expect(csp_session.token).to eq token
        expect(csp_session.token_exp).to_not be_nil
        expect(csp_session.token_exp).to be_within(1.second).of 899.seconds.from_now
        expect(csp_session.id_token).to eq expected_id_token
      end
    end

    context :user_does_not_exist do
      it 'does not sign in user' do
        post auth_endpoint
        follow_redirect!
        expect(response.location).to eq organizations_url
        expect(response).to be_redirect
        follow_redirect!
        expect(response).to be_redirect
      end

      it 'sets authentication token' do
        post auth_endpoint
        follow_redirect!
        csp_session = CspSession.new(request.session)
        expect(csp_session.current).to eq csp_name
        expect(csp_session.token).to eq token
        expect(csp_session.token_exp).to_not be_nil
        expect(csp_session.token_exp).to be_within(1.second).of 899.seconds.from_now
        expect(csp_session.id_token).to eq expected_id_token
        expect(csp_session.user).to be_nil
      end
    end
  end
end
