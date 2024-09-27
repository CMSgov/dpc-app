# frozen_string_literal: true

require 'rails_helper'

RSpec.describe VerifyAoJob, type: :job do
  include ActiveJob::TestHelper

  describe :perform do
    context :cpi_gateway_client_error do
      before do
        user = create(:user, pac_id: '900111111', verification_status: :approved)
        create(:ao_org_link, user:, last_checked_at: 6.days.ago)
        cpi_api_gateway_client_class = class_double(CpiApiGatewayClient).as_stubbed_const
        cpi_api_gateway_client = double(CpiApiGatewayClient)
        expect(cpi_api_gateway_client_class).to receive(:new).at_least(:once).and_return(cpi_api_gateway_client)
        expect(cpi_api_gateway_client).to receive(:fetch_profile).and_raise(
          OAuth2::Error, Faraday::Response.new(status: 500)
        )
      end
      it 'handles OAuth2::Error raised by service.check_ao_eligibility in the perform method' do
        expect(Rails.logger).to receive(:error).with(['API Gateway Error during AO Verification'])
        VerifyAoJob.perform_now
      end
    end

    context :chained do
      before do
        allow(ENV).to receive(:fetch).and_call_original
        expect(ENV)
          .to receive(:fetch)
          .with('VERIFICATION_MAX_RECORDS', '10')
          .and_return('4').at_least(4)
        user = create(:user, pac_id: '900111111', verification_status: :approved)
        10.times do |n|
          create(:ao_org_link, user:, last_checked_at: (n + 6).days.ago)
        end
      end
      it 'should keep calling until done' do
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 10
        VerifyAoJob.perform_now
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 6
        assert_enqueued_with(job: VerifyAoJob)
        perform_enqueued_jobs
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 2
        assert_enqueued_with(job: VerifyAoJob)
        perform_enqueued_jobs
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 0
        assert_enqueued_with(job: VerifyProviderOrganizationJob)
      end
    end

    context :no_failures do
      before do
        allow(ENV).to receive(:fetch).and_call_original
        user = create(:user, pac_id: '900111111', verification_status: :approved)
        10.times do |n|
          create(:ao_org_link, user:, last_checked_at: (n + 6).days.ago)
        end
      end
      it 'should only update VERIFICATION_MAX_RECORDS ao_org_links' do
        expect(ENV)
          .to receive(:fetch)
          .with('VERIFICATION_MAX_RECORDS', '10')
          .and_return('4')
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 10
        VerifyAoJob.perform_now
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 6
      end
      it 'should only update ao_org_links checked VERIFICATION_LOOKBACK_HOURS ago or more' do
        ten_days_in_hours = 10 * 24
        expect(ENV)
          .to receive(:fetch)
          .with('VERIFICATION_LOOKBACK_HOURS', '144')
          .and_return(ten_days_in_hours.to_s)
        expect(AoOrgLink.where(last_checked_at: ..10.days.ago).count).to eq 6
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 10
        VerifyAoJob.perform_now
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 4
      end
      it 'should not invalidate ao_org_links or users' do
        VerifyAoJob.perform_now
        expect(AoOrgLink.where(verification_status: false)).to be_empty
        expect(User.where(verification_status: 'rejected')).to be_empty
      end
      it 'should update the links and users last_checked_at' do
        links_to_check = AoOrgLink.where(last_checked_at: ..6.days.ago,
                                         verification_status: true)

        VerifyAoJob.perform_now
        links_to_check.each do |link|
          link.reload
          link.provider_organization.reload
          expect(link.last_checked_at).to be > 1.day.ago
          expect(link.provider_organization.last_checked_at).to be > 1.day.ago
          expect(link.user.last_checked_at).to be > 1.day.ago
        end
      end
      it 'should not update if any object fails to update' do
        links_to_check = AoOrgLink.where(last_checked_at: ..6.days.ago,
                                         verification_status: true)
        expect_any_instance_of(User).to receive(:update!).and_raise('error')
        expect do
          VerifyAoJob.perform_now
        end.to raise_error(RuntimeError, 'error')

        links_to_check.each do |link|
          link.reload
          expect(link.last_checked_at).to be < 6.days.ago
          expect(link.provider_organization.last_checked_at).to be_nil
          expect(link.user.last_checked_at).to be_nil
        end
      end
      it 'should set the current provider_organization and user' do
        links_to_check = AoOrgLink.where(verification_status: true)
        user_id = links_to_check.first.user.id
        allow(CurrentAttributes).to receive(:save_organization_attributes)
        expect(CurrentAttributes).to receive(:save_organization_attributes) do |org_from_job, user_from_job|
          expect(links_to_check.pluck(:provider_organization_id)).to include(org_from_job.id)
          expect(user_id).to equal(user_from_job.id)
        end
        allow(CurrentAttributes).to receive(:save_user_attributes)
        expect(CurrentAttributes).to receive(:save_user_attributes) do |user_from_job|
          expect(user_id).to equal(user_from_job.id)
        end
        VerifyAoJob.perform_now
      end
    end
    context :ao_has_waiver do
      let(:user) { create(:user, pac_id: '900777777', verification_status: :approved) }
      let(:provider_organization) { create(:provider_organization, npi: '900111111', verification_status: :approved) }
      let!(:link) { create(:ao_org_link, last_checked_at: 8.days.ago, user:, provider_organization:) }

      it 'should log when an AO has a waiver' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info)
          .with(['Authorized official has a waiver',
                 { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                   actionType: LoggingConstants::ActionType::AoHasWaiver }])
        VerifyAoJob.perform_now
      end
    end
    context :org_has_waiver do
      let(:user) { create(:user, pac_id: '900111111', verification_status: :approved) }
      let(:provider_organization) { create(:provider_organization, npi: '3098168743', verification_status: :approved) }
      let!(:link) { create(:ao_org_link, last_checked_at: 8.days.ago, user:, provider_organization:) }

      it 'should log when a provider org has a waiver' do
        allow(Rails.logger).to receive(:info)
        expect(Rails.logger).to receive(:info)
          .with(['Organization has a waiver',
                 { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                   actionType: LoggingConstants::ActionType::OrgHasWaiver }])
        VerifyAoJob.perform_now
      end
    end
    context :failures do
      def expect_log_for(link, reason)
        expect(Rails.logger).to receive(:info)
          .with(['VerifyAoJob Check Fail',
                 { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                   actionType: LoggingConstants::ActionType::FailCpiApiGwCheck,
                   verificationReason: reason,
                   authorizedOfficial: link.user.id,
                   providerOrganization: link.provider_organization.id }])
      end

      # rubocop:disable Metrics/AbcSize
      def expect_audits(link, also: [])
        expected_comment = LoggingConstants::ActionContext::BatchVerificationCheck

        expect(link.audits.length).to eq 1
        expect(link.audits.first.comment).to eq expected_comment

        if also.include?(:user)
          expect(link.user.audits.length).to eq 1
          expect(link.user.audits.first.comment).to eq expected_comment
        else
          expect(link.user.audits.length).to eq 0
        end

        if also.include?(:org)
          expect(link.provider_organization.audits.length).to eq 1
          expect(link.provider_organization.audits.first.comment).to eq expected_comment
        else
          expect(link.provider_organization.audits.length).to eq 0
        end
      end
      # rubocop:enable Metrics/AbcSize

      context :ao_med_sanctions do
        let(:user) { create(:user, pac_id: '900666666', verification_status: :approved) }
        let(:links) { [] }
        before do
          3.times do |n|
            provider_organization = create(:provider_organization, verification_status: :approved)
            links << create(:ao_org_link, last_checked_at: (n + 4).days.ago, user:, provider_organization:)
          end
        end
        it "should update user and all user's orgs and links" do
          expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 1
          VerifyAoJob.perform_now
          links.each do |link|
            link.reload
            expect(link.verification_status).to be false
            expect(link.verification_reason).to eq 'ao_med_sanctions'
            expect(link.user.verification_status).to eq 'rejected'
            expect(link.user.verification_reason).to eq 'ao_med_sanctions'
            expect(link.provider_organization.verification_status).to eq 'rejected'
            expect(link.provider_organization.verification_reason).to eq 'ao_med_sanctions'
          end
        end
        it 'should log user check failed' do
          allow(Rails.logger).to receive(:info)
          links.each do |link|
            expect_log_for(link, 'ao_med_sanctions')
          end
          VerifyAoJob.perform_now
          links.each do |link|
            expect_audits(link, also: %i[user org])
          end
        end
        it 'should not update former org/link' do
          user = create(:user, pac_id: '900666666', verification_status: :approved)
          provider_organization = create(:provider_organization, verification_status: :approved)
          link = create(:ao_org_link, last_checked_at: 8.days.ago, user:, provider_organization:)
          former_org = create(:provider_organization, verification_status: :approved)
          former_link = create(:ao_org_link, verification_status: false,
                                             verification_reason: 'user_not_authorized_official',
                                             last_checked_at: 8.days.ago, user:, provider_organization: former_org)
          VerifyAoJob.perform_now
          link.reload
          expect(link.verification_status).to be false
          expect(link.verification_reason).to eq 'ao_med_sanctions'
          expect(link.user.verification_status).to eq 'rejected'
          expect(link.user.verification_reason).to eq 'ao_med_sanctions'
          expect(link.provider_organization.verification_status).to eq 'rejected'
          expect(link.provider_organization.verification_reason).to eq 'ao_med_sanctions'
          expect(link.verification_status).to be false
          former_link.reload
          expect(former_link.verification_reason).to eq 'user_not_authorized_official'
          former_org.reload
          expect(former_org.verification_status).to eq 'approved'
        end
      end
      context :no_approved_enrollment do
        let(:user) { create(:user, pac_id: '900111111', verification_status: :approved) }
        let(:provider_organization) do
          create(:provider_organization, npi: '3782297014', verification_status: :approved)
        end
        let!(:link) { create(:ao_org_link, last_checked_at: 8.days.ago, user:, provider_organization:) }

        it 'should update org and link on fails no approved enrollments' do
          VerifyAoJob.perform_now
          link.reload
          expect(link.verification_status).to be false
          expect(link.verification_reason).to eq 'no_approved_enrollment'
          expect(link.user.verification_status).to eq 'approved'
          expect(link.provider_organization.verification_status).to eq 'rejected'
          expect(link.provider_organization.verification_reason).to eq 'no_approved_enrollment'
        end

        it 'should log check failed' do
          allow(Rails.logger).to receive(:info)
          expect_log_for(link, 'no_approved_enrollment')
          VerifyAoJob.perform_now
          expect_audits(link, also: [:org])
        end
      end
      context :user_not_authorized_official do
        let(:user) { create(:user, pac_id: 'bad-id', verification_status: :approved) }
        let(:provider_organization) { create(:provider_organization, verification_status: :approved) }
        let!(:link) { create(:ao_org_link, last_checked_at: 8.days.ago, user:, provider_organization:) }
        it 'should update only link and org date' do
          VerifyAoJob.perform_now
          link.reload
          expect(link.verification_status).to be false
          expect(link.verification_reason).to eq 'user_not_authorized_official'
          expect(link.user.verification_status).to eq 'approved'
          expect(link.provider_organization.verification_status).to eq 'approved'
          expect(link.provider_organization.last_checked_at).to be > 1.day.ago
        end
        it 'should log check failed' do
          allow(Rails.logger).to receive(:info)
          expect_log_for(link, 'user_not_authorized_official')
          VerifyAoJob.perform_now
          expect_audits(link)
        end
      end
      context :org_med_sanctions do
        let(:provider_organization) { create(:provider_organization, npi: '3598564557') }
        let(:links) { [] }
        before do
          3.times do
            links << create(:ao_org_link, provider_organization:)
          end
          links.first.update!(last_checked_at: 8.days.ago)
        end
        it 'should update org and link' do
          links.first.update!(last_checked_at: 8.days.ago)
          VerifyAoJob.perform_now
          links.each do |link|
            link.reload
            expect(link.verification_status).to be false
            expect(link.verification_reason).to eq 'org_med_sanctions'
            expect(link.user.verification_status).to_not eq 'rejected'
            expect(link.provider_organization.verification_status).to eq 'rejected'
            expect(link.provider_organization.verification_reason).to eq 'org_med_sanctions'
            expect(link.last_checked_at).to be > 1.day.ago
          end
        end
        it 'should log checks failed' do
          allow(Rails.logger).to receive(:info)
          links.each do |link|
            expect_log_for(link, 'org_med_sanctions')
          end
          VerifyAoJob.perform_now
          links.each do |link|
            expect_audits(link, also: [:org])
          end
        end
      end
      it 'should not update if any object fails to update' do
        user = create(:user, pac_id: '900111111', verification_status: :approved)
        provider_organization = create(:provider_organization, npi: '3782297014', verification_status: :approved)
        expect_any_instance_of(ProviderOrganization).to receive(:update!).and_raise('error')
        link = create(:ao_org_link, last_checked_at: 8.days.ago, user:, provider_organization:)
        expect do
          VerifyAoJob.perform_now
        end.to raise_error(RuntimeError, 'error')
        link.reload
        user.reload
        provider_organization.reload
        expect(link.verification_status).to be true
        expect(user.verification_status).to eq 'approved'
        expect(provider_organization.verification_status).to eq 'approved'
      end
    end
  end
end
