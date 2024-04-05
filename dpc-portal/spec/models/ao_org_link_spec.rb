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
    let(:invitation) { build(:invitation) }
    let(:ao_org_link) { build(:ao_org_link, user:, provider_organization: provider_organization1, invitation:) }

    it 'has foreign keys' do
      expect(ao_org_link.user).to eq user
      expect(ao_org_link.provider_organization).to eq provider_organization1
      expect(ao_org_link.invitation).to eq invitation
    end

    it 'does not allow for duplicate invitations' do
      create(:ao_org_link, user:, provider_organization: provider_organization1, invitation:)
      duplicate = build(:ao_org_link, user:, provider_organization: provider_organization2, invitation:)
      expect(duplicate.valid?).to be_falsy
      expect(duplicate.errors.full_messages).to include 'Invitation already used by another AoOrgLink.'
    end
  end
end
