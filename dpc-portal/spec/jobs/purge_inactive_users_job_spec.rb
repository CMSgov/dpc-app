# frozen_string_literal: true

require 'rails_helper'
require 'support/login_support'

RSpec.describe PurgeInactiveUsersJob, type: :job do
  include ActiveSupport::Testing::TimeHelpers
  include LoginSupport

  describe :perform do
    CspUtils::CODES_TO_DISPLAY.each do |provider, display_name|
      context "using #{display_name}" do
        let(:csp) { Csp.find_by!(name: provider) }

        it 'does not purge a user who signed in less than 6 years ago' do
          user = create_user_with_csp(csp: provider)
          create(:user_sign_in, user:, csp:, last_login_at: 5.years.ago)

          PurgeInactiveUsersJob.perform_now

          expect(User.exists?(user.id)).to be true
        end

        it 'purges a user whose only sign-in is more than 6 years old' do
          user = create_user_with_csp(csp: provider)
          create(:user_sign_in, user:, csp:, last_login_at: 7.years.ago)

          PurgeInactiveUsersJob.perform_now

          expect(User.exists?(user.id)).to be false
        end

        it 'deletes org links, csp_users, user_emails, and all sign-in rows for a purged user' do
          user = create_user_with_csp(csp: provider)
          create(:user_sign_in, user:, csp:, last_login_at: 7.years.ago)
          organization = create(:provider_organization)
          ao_link = create(:ao_org_link, user:, provider_organization: organization)
          csp_user = user.csp_users.find_by!(csp:)
          email = create(:user_email, csp_user:)

          PurgeInactiveUsersJob.perform_now

          expect(AoOrgLink.where(id: ao_link.id)).to be_empty
          expect(CspUser.where(id: csp_user.id)).to be_empty
          expect(UserEmail.where(id: email.id)).to be_empty
          expect(UserSignIn.where(user_id: user.id)).to be_empty
        end

        it 'nulls user_id on csp_user_verifications and credential_audit_logs instead of deleting them' do
          user = create_user_with_csp(csp: provider)
          create(:user_sign_in, user:, csp:, last_login_at: 7.years.ago)
          verification = create(:csp_user_verification, user:, csp:)
          audit_log = create(:credential_audit_log, user:)

          PurgeInactiveUsersJob.perform_now

          expect(verification.reload.user_id).to be_nil
          expect(audit_log.reload.user_id).to be_nil
        end

        it 'nulls invited_by_id and terms_of_service_accepted_by_id rather than deleting those records' do
          user = create_user_with_csp(csp: provider)
          create(:user_sign_in, user:, csp:, last_login_at: 7.years.ago)
          organization = create(:provider_organization)
          invitation = create(:invitation, invited_by: user, provider_organization: organization,
                                           invitation_type: :authorized_official)
          organization.update!(terms_of_service_accepted_by_id: user.id, terms_of_service_accepted_at: Time.now)

          PurgeInactiveUsersJob.perform_now

          expect(invitation.reload.invited_by_id).to be_nil
          expect(organization.reload.terms_of_service_accepted_by_id).to be_nil
          expect(Invitation.exists?(invitation.id)).to be true
          expect(ProviderOrganization.exists?(organization.id)).to be true
        end

        it "writes a purge log with that CSP's last_login_at" do
          user = create_user_with_csp(csp: provider)
          last_login_at = 7.years.ago
          create(:user_sign_in, user:, csp:, last_login_at:)
          user_id = user.id

          freeze_time do
            PurgeInactiveUsersJob.perform_now

            log = UserPurgeLog.find_by(user_id: user_id)
            expect(log).to be_present
            expect(log.last_login_at).to be_within(1.second).of(last_login_at)
            expect(log.purged_at).to eq(Time.now)
          end
        end

        it 'does not write a purge log for a user who is not purged' do
          user = create_user_with_csp(csp: provider)
          create(:user_sign_in, user:, csp:, last_login_at: 5.years.ago)

          PurgeInactiveUsersJob.perform_now

          expect(UserPurgeLog.where(user_id: user.id)).to be_empty
        end

        it 'logs a failure and still purges remaining eligible users when one purge raises' do
          failing_user = create_user_with_csp(csp: provider)
          create(:user_sign_in, user: failing_user, csp:, last_login_at: 7.years.ago)

          healthy_user = create_user_with_csp(csp: provider)
          create(:user_sign_in, user: healthy_user, csp:, last_login_at: 7.years.ago)

          allow(UserPurgeLog).to receive(:create!).and_wrap_original do |original, **args|
            raise ActiveRecord::RecordInvalid if args[:user_id] == failing_user.id

            original.call(**args)
          end

          expect(Rails.logger).to receive(:error).with(
            ['User purge failed', hash_including(actionType: LoggingConstants::ActionType::UserPurgeFailed)]
          )

          PurgeInactiveUsersJob.perform_now

          expect(User.exists?(failing_user.id)).to be true
          expect(User.exists?(healthy_user.id)).to be false
        end

        context 'with a second CSP' do
          let(:other_csp_name) { (CspUtils::CODES_TO_DISPLAY.keys - [provider]).sample.to_s }
          let(:other_csp) { Csp.find_by(name: other_csp_name) || create(:csp, name: other_csp_name) }

          it 'does not purge a user who is stale on this CSP but recent on another' do
            user = create_user_with_csp(csp: provider)
            create(:user_sign_in, user:, csp:, last_login_at: 7.years.ago)
            create(:user_sign_in, user:, csp: other_csp, last_login_at: 1.year.ago)

            PurgeInactiveUsersJob.perform_now

            expect(User.exists?(user.id)).to be true
          end

          it 'purges a user who is stale across both CSPs they have signed in with' do
            user = create_user_with_csp(csp: provider)
            create(:user_sign_in, user:, csp:, last_login_at: 8.years.ago)
            create(:user_sign_in, user:, csp: other_csp, last_login_at: 7.years.ago)

            PurgeInactiveUsersJob.perform_now

            expect(User.exists?(user.id)).to be false
          end

          it 'writes a purge log using the most recent last_login_at across both CSPs' do
            user = create_user_with_csp(csp: provider)
            create(:user_sign_in, user:, csp:, last_login_at: 8.years.ago)
            most_recent_login = 7.years.ago
            create(:user_sign_in, user:, csp: other_csp, last_login_at: most_recent_login)
            user_id = user.id

            freeze_time do
              PurgeInactiveUsersJob.perform_now

              log = UserPurgeLog.find_by(user_id: user_id)
              expect(log.last_login_at).to be_within(1.second).of(most_recent_login)
            end
          end
        end
      end
    end
  end
end
