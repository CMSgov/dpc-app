# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CdOrgLink, type: :model do
  let(:organization) { build(:provider_organization) }
  let(:credential_delegate) { build(:user) }
  let(:new_cd_invite) { build(:invitation) }
  let(:cd_org_link) do
    build(:cd_org_link, user: credential_delegate, provider_organization: organization, invitation: new_cd_invite)
  end

  it 'has foreign keys' do
    expect(cd_org_link.user).to eq credential_delegate
    expect(cd_org_link.provider_organization).to eq organization
    expect(cd_org_link.invitation).to eq new_cd_invite
  end
end
