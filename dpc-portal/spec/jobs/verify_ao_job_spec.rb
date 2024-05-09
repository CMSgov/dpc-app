# frozen_string_literal: true

require 'rails_helper'

RSpec.describe VerifyAoJob, type: :job do
  describe :perform do
    context :no_failures do
      before do
        allow(ENV).to receive(:fetch).and_call_original
        user = create(:user, pac_id: '900111111', verification_status: :approved)
        10.times do |n|
          create(:ao_org_link, user:, last_checked_at: (n + 6).days.ago)
        end
      end
      it 'should only update MAX_RECORDS ao_org_links' do
        expect(ENV)
          .to receive(:fetch)
          .with('MAX_RECORDS', '10')
          .and_return('4')
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 10
        VerifyAoJob.perform_now
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 6
      end
      it 'should only update ao_org_links checked LOOKBACK_HOURS ago or more' do
        ten_days_in_hours = 10 * 24
        expect(ENV)
          .to receive(:fetch)
          .with('LOOKBACK_HOURS', '144')
          .and_return(ten_days_in_hours.to_s)
        expect(AoOrgLink.where(last_checked_at: ..10.days.ago).count).to eq 6
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 10
        VerifyAoJob.perform_now
        expect(AoOrgLink.where(last_checked_at: ..6.days.ago).count).to eq 4
      end
      it 'should not invalidate valid ao_org_link objects' do
        VerifyAoJob.perform_now
        expect(AoOrgLink.where(verification_status: false)).to be_empty
      end
    end
    context :failures do
      it "should update user and all user's orgs and links on failed ao med sanction" do
        user = create(:user, pac_id: '900666666', verification_status: :approved)
        links = []
        3.times do |n|
          provider_organization = create(:provider_organization, verification_status: :approved)
          links << create(:ao_org_link, last_checked_at: (n + 4).days.ago, user:, provider_organization:)
        end
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
      it 'should not update former org/link on user med sanction' do
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

      it 'should update org and link on fails no approved enrollments' do
        user = create(:user, pac_id: '900111111', verification_status: :approved)
        provider_organization = create(:provider_organization, npi: '3782297014', verification_status: :approved)
        link = create(:ao_org_link, last_checked_at: 8.days.ago, user:, provider_organization:)
        VerifyAoJob.perform_now
        link.reload
        expect(link.verification_status).to be false
        expect(link.verification_reason).to eq 'no_approved_enrollment'
        expect(link.user.verification_status).to eq 'approved'
        expect(link.provider_organization.verification_status).to eq 'rejected'
        expect(link.provider_organization.verification_reason).to eq 'no_approved_enrollment'
      end
      it 'should update only link on fails no longer ao' do
        user = create(:user, pac_id: 'bad-id', verification_status: :approved)
        provider_organization = create(:provider_organization, verification_status: :approved)
        link = create(:ao_org_link, last_checked_at: 8.days.ago, user:, provider_organization:)
        VerifyAoJob.perform_now
        link.reload
        expect(link.verification_status).to be false
        expect(link.verification_reason).to eq 'user_not_authorized_official'
        expect(link.user.verification_status).to eq 'approved'
        expect(link.provider_organization.verification_status).to eq 'approved'
      end
    end
  end
end
