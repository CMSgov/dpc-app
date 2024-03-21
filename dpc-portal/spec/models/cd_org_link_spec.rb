# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CdOrgLink, type: :model do
  let(:provider_organization) { build(:provider_organization) }
  let(:user) do
    User.new(given_name: 'Bob', family_name: 'Hodges', email: 'bob@example.com')
  end
  let(:invitation) do
    Invitation.new(invited_given_name: 'Bob', invited_family_name: 'Hodges', invited_email: 'bob@example.com',
                   verification_code: 'ABC123')
  end
  let(:cd_org_link) do
    build(:cd_org_link, user:, provider_organization:, invitation:)
  end

  it 'has foreign keys' do
    expect(cd_org_link.user).to eq user
    expect(cd_org_link.provider_organization).to eq provider_organization
    expect(cd_org_link.invitation).to eq invitation
  end

  it 'shows attributes' do
    attrs = cd_org_link.show_attributes
    expect(attrs['full_name']).to eq 'Bob Hodges'
    expect(attrs['email']).to eq 'bob@example.com'
    expect(attrs['verification_code']).to eq 'ABC123'
    expect(attrs['activated_at']).to eq cd_org_link.created_at.to_s
  end
end
