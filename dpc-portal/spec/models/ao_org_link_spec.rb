# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AoOrgLink, type: :model do
  let(:user) { build(:user) }

  describe 'has no invitation' do
    let(:provider_organization) { build(:provider_organization) }
    let(:ao_org_link) { build(:ao_org_link, user:, provider_organization:) }

    it 'has foreign keys' do
      expect(ao_org_link.user).to eq user
      expect(ao_org_link.provider_organization).to eq provider_organization
    end

    it 'does not allow for duplicate user-org pairs' do
      create(:ao_org_link, user:, provider_organization:)
      duplicate = build(:ao_org_link, user:, provider_organization:)
      expect(duplicate.valid?).to be_falsy
      expect(duplicate.errors.full_messages).to include 'User already exists for this provider.'
    end
  end

  describe 'has an invitation' do
    let(:provider_organization1) { build(:provider_organization) }
    let(:provider_organization2) { build(:provider_organization) }
    let(:invitation) { create(:invitation, :ao) }
    let(:ao_org_link) { build(:ao_org_link, user:, provider_organization: provider_organization1, invitation:) }

    it 'has foreign keys' do
      expect(ao_org_link.user).to eq user
      expect(ao_org_link.provider_organization).to eq provider_organization1
      expect(ao_org_link.invitation).to eq invitation
    end

    it 'allows for multiple nil invitations' do
      create(:ao_org_link, user:, provider_organization: provider_organization1, invitation: nil)
      duplicate = build(:ao_org_link, user:, provider_organization: provider_organization2, invitation: nil)
      expect(duplicate).to be_valid
    end

    it 'does not allow for duplicate invitations' do
      create(:ao_org_link, user:, provider_organization: provider_organization1, invitation:)
      duplicate = build(:ao_org_link, user:, provider_organization: provider_organization2, invitation:)
      expect(duplicate.valid?).to be_falsy
      expect(duplicate.errors.full_messages).to include 'Invitation already used by another AoOrgLink.'
    end
  end

  describe 'sets defaults' do
    let(:provider_organization) { build(:provider_organization) }
    let(:ao_org_link) { create(:ao_org_link, user:, provider_organization:) }

    it 'defaults verification_status to true' do
      expect(ao_org_link.verification_status).to be true
    end

    it 'defaults last_checked_at to now' do
      expect(ao_org_link.last_checked_at).to be_within(10.seconds).of Time.now
    end
  end

  describe 'check validations' do
    let(:provider_organization) { build(:provider_organization) }
    let(:ao_org_link) { create(:ao_org_link, user:, provider_organization:) }

    it 'fails on invalid verification_reason' do
      expect do
        ao_org_link.verification_reason = :fake_status
      end.to raise_error(ArgumentError)
    end

    it 'allows good verification_reason' do
      expect do
        ao_org_link.verification_reason = :user_not_authorized_official
        ao_org_link.save
      end.not_to raise_error
    end

    it 'allows blank verification_reason' do
      expect do
        ao_org_link.verification_reason = ''
        ao_org_link.save
      end.not_to raise_error
    end

    it 'allows nil verification_reason' do
      expect do
        ao_org_link.verification_reason = nil
        ao_org_link.save
      end.not_to raise_error
    end
  end

  describe :audits do
    let(:ao_org_link) { create(:ao_org_link, verification_status: true) }
    it 'should audit verification_status' do
      ao_org_link.update(verification_status: false)
      expect(ao_org_link.audits.count).to eq 1
    end
    it 'should audit verification_reason' do
      ao_org_link.update(verification_reason: :org_med_sanctions)
      expect(ao_org_link.audits.count).to eq 1
    end
    it 'should audit verification_status and _reason together' do
      ao_org_link.update(verification_status: false, verification_reason: :org_med_sanctions)
      expect(ao_org_link.audits.count).to eq 1
    end
  end
end
