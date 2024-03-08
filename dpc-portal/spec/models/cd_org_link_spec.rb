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
    expect(cd_org_link.user).not_to be(nil)
    expect(cd_org_link.provider_organization).not_to be(nil)
    expect(cd_org_link.invitation).not_to be(nil)
  end
end
