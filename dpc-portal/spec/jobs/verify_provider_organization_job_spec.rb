# frozen_string_literal: true

require 'rails_helper'

RSpec.describe VerifyProviderOrganizationJob, type: :job do
  include ActiveJob::TestHelper

  describe :perform do
    context :chained do
      before do
        allow(ENV).to receive(:fetch).and_call_original
        10.times do |n|
          link = create(:ao_org_link, last_checked_at: (n + 6).days.ago)
          link.provider_organization.update!(verification_status: 'approved',
                                             last_checked_at: (n + 6).days.ago)
        end
      end
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
        expect(ProviderOrganization.where(last_checked_at: ..6.days.ago).count).to eq 10
        VerifyProviderOrganizationJob.perform_now
        expect(ProviderOrganization.where(last_checked_at: ..6.days.ago).count).to eq 6
        assert_enqueued_with(job: VerifyProviderOrganizationJob)
        perform_enqueued_jobs
        expect(ProviderOrganization.where(last_checked_at: ..6.days.ago).count).to eq 2
        assert_enqueued_with(job: VerifyProviderOrganizationJob)
        perform_enqueued_jobs
        expect(ProviderOrganization.where(last_checked_at: ..6.days.ago).count).to eq 0
        assert_enqueued_with(job: VerifyProviderOrganizationJob)
        perform_enqueued_jobs
        expect(ProviderOrganization.where(last_checked_at: ..6.days.ago).count).to eq 0
        assert_no_enqueued_jobs
      end
    end

    context :no_failures do
      before do
        allow(ENV).to receive(:fetch).and_call_original
        10.times do |n|
          link = create(:ao_org_link, last_checked_at: (n + 6).days.ago)
          link.provider_organization.update!(verification_status: 'approved',
                                             last_checked_at: (n + 6).days.ago)
        end
      end
      it 'should only update VERIFICATION_MAX_RECORDS provider_organizations' do
        expect(ENV)
          .to receive(:fetch)
          .with('VERIFICATION_MAX_RECORDS', '10')
          .and_return('4')
        expect(ProviderOrganization.where(last_checked_at: ..6.days.ago).count).to eq 10
        VerifyProviderOrganizationJob.perform_now
        expect(ProviderOrganization.where(last_checked_at: ..6.days.ago).count).to eq 6
      end
      it 'should only update provider_organizations checked VERIFICATION_LOOKBACK_HOURS ago or more' do
        ten_days_in_hours = 10 * 24
        expect(ENV)
          .to receive(:fetch)
          .with('VERIFICATION_LOOKBACK_HOURS', '144')
          .and_return(ten_days_in_hours.to_s)
        expect(ProviderOrganization.where(last_checked_at: ..10.days.ago).count).to eq 6
        expect(ProviderOrganization.where(last_checked_at: ..6.days.ago).count).to eq 10
        VerifyProviderOrganizationJob.perform_now
        expect(ProviderOrganization.where(last_checked_at: ..6.days.ago).count).to eq 4
      end
      it 'should not invalidate provider_organizations' do
        VerifyProviderOrganizationJob.perform_now
        expect(ProviderOrganization.where(verification_status: 'rejected')).to be_empty
      end
      it 'should update provider_organizations last_checked_at' do
        orgs_to_check = ProviderOrganization.where(last_checked_at: ..6.days.ago,
                                                   verification_status: true)

        VerifyProviderOrganizationJob.perform_now
        orgs_to_check.each do |org|
          org.reload
          expect(org.last_checked_at).to be > 1.day.ago
        end
      end
    end
    context :failures do
      it 'should update org and link if org has med sanctions' do
        provider_organization = create(:provider_organization, last_checked_at: 8.days.ago, npi: '3598564557',
                                                               verification_status: :approved)
        links = []
        3.times do
          links << create(:ao_org_link, provider_organization:)
        end
        VerifyProviderOrganizationJob.perform_now
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
      it 'should update org and links if org has no enrollments' do
        provider_organization = create(:provider_organization, last_checked_at: 8.days.ago, npi: '3782297014',
                                                               verification_status: :approved)
        links = []
        3.times do
          links << create(:ao_org_link, provider_organization:)
        end
        VerifyProviderOrganizationJob.perform_now
        links.each do |link|
          link.reload
          expect(link.verification_status).to be false
          expect(link.verification_reason).to eq 'no_approved_enrollment'
          expect(link.user.verification_status).to_not eq 'rejected'
          expect(link.provider_organization.verification_status).to eq 'rejected'
          expect(link.provider_organization.verification_reason).to eq 'no_approved_enrollment'
        end
      end
      it 'should not update former link if no enrollments' do
        provider_organization = create(:provider_organization, last_checked_at: 8.days.ago, npi: '3782297014',
                                                               verification_status: :approved)
        link = create(:ao_org_link, provider_organization:)
        former_link = create(:ao_org_link, provider_organization:, verification_status: false,
                                           verification_reason: 'ao_med_sanctions')
        VerifyProviderOrganizationJob.perform_now
        link.reload
        expect(link.verification_status).to be false
        expect(link.verification_reason).to eq 'no_approved_enrollment'
        former_link.reload
        expect(former_link.verification_status).to be false
        expect(former_link.verification_reason).to eq 'ao_med_sanctions'
      end
      it 'should not update if any object fails to update' do
        provider_organization = create(:provider_organization, last_checked_at: 8.days.ago, npi: '3598564557',
                                                               verification_status: :approved)
        user = create(:user, pac_id: '900111111', verification_status: :approved)
        expect_any_instance_of(AoOrgLink).to receive(:update!).and_raise('error')
        link = create(:ao_org_link, provider_organization:)
        expect do
          VerifyProviderOrganizationJob.perform_now
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
