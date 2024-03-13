# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AoOrgLink, type: :model do
  let(:provider_organization) { build(:provider_organization) }
  let(:user) { User.new(given_name: 'Bob', family_name: 'Hodges', email: 'bob@example.com') }
  let(:ao_org_link) { build(:ao_org_link, user:, provider_organization:) }

  it 'has foreign keys' do
    expect(ao_org_link.user).to eq user
    expect(ao_org_link.provider_organization).to eq provider_organization
  end

  it 'shows attributes' do
    attrs = ao_org_link.show_attributes
    expect(attrs['full_name']).to eq 'Bob Hodges'
    expect(attrs['email']).to eq 'bob@example.com'
  end
end
