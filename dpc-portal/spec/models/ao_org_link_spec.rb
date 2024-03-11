# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AoOrgLink, type: :model do
  let(:organization) { build(:provider_organization) }
  let(:authorized_official) { build(:user) }
  let(:ao_org_link) { build(:ao_org_link, user: authorized_official, provider_organization: organization) }

  it 'has foreign keys' do
    expect(ao_org_link.user).to eq authorized_official
    expect(ao_org_link.provider_organization).to eq organization
  end
end
