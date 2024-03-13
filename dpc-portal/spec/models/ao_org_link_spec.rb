# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AoOrgLink, type: :model do
  let(:provider_organization) { build(:provider_organization) }
  let(:user) { build(:user) }
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
