# frozen_string_literal: true

require 'rails_helper'

# These responses are the same across all CSPs.
RSpec.describe 'CSP', type: :request do
  describe 'Get /auth/failure' do
    context 'access denied' do
      let(:auth_failure_path) { '/auth/failure?message=access_denied&strategy=csp' }
      it 'should redirect to sign-in' do
        get auth_failure_path
        expect(response.location).to eq(sign_in_url)
      end

      it 'should log on failure' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User cancelled login',
                                                     hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                    actionType: LoggingConstants::ActionType::UserCancelledLogin,
                                                                    csp: 'csp',
                                                                    timestamp: a_kind_of(String))])
        get auth_failure_path
      end
    end

    context 'verification pending' do
      let(:auth_failure_path) { '/auth/failure?message=pending&strategy=csp' }

      it 'should redirect to sign-in with the pending alert' do
        get auth_failure_path
        expect(response.location).to eq(sign_in_url)
        expect(flash[:alert]).to eq(CspErrorHandling::VERIFICATION_PENDING)
      end

      it 'should log the pending session' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info).with(['User verification session pending',
                                                     hash_including(actionContext: LoggingConstants::ActionContext::Authentication,
                                                                    actionType: LoggingConstants::ActionType::FailedLogin,
                                                                    csp: 'csp',
                                                                    timestamp: a_kind_of(String))])
        get auth_failure_path
      end

      context 'when registering with an invitation' do
        before do
          allow_any_instance_of(CspController).to receive(:invitation_match).and_return(invitation)
        end

        context 'as an authorized official' do
          let(:invitation) { create(:invitation, :ao) }

          it 'should send the person back to accept the invitation with the pending alert' do
            get auth_failure_path
            expect(response.location).to eq accept_organization_invitation_url(invitation.provider_organization_id,
                                                                               invitation.id,
                                                                               invitation.token)
            expect(flash[:alert]).to eq(CspErrorHandling::VERIFICATION_PENDING)
          end

          it 'should log it as part of registration' do
            allow(Rails.logger).to receive(:info)
            expect(Rails.logger).to receive(:info).with(['User verification session pending',
                                                         hash_including(actionContext: LoggingConstants::ActionContext::Registration,
                                                                        actionType: LoggingConstants::ActionType::FailedLogin)])
            get auth_failure_path
          end
        end

        context 'as a credential delegate' do
          let(:invitation) { create(:invitation, :cd) }

          it 'should send the person back to confirm the invitation with the pending alert' do
            get auth_failure_path
            expect(response.location).to eq confirm_cd_organization_invitation_url(invitation.provider_organization_id,
                                                                                   invitation.id,
                                                                                   invitation.token)
            expect(flash[:alert]).to eq(CspErrorHandling::VERIFICATION_PENDING)
          end
        end
      end
    end

    context 'verification failure with invalid CSP' do
      let(:auth_failure_path) { '/auth/failure?message=verification_failure&strategy=csp' }
      it 'should render not found' do
        get auth_failure_path
        expect(response).to be_not_found
      end
    end
  end

  describe 'Get /auth/no_account' do
    it 'should show logout button' do
      get '/auth/no_account'
      expect(response.body).to include 'Back to sign in'
    end
  end
end
