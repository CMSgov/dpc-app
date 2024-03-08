# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CdOrgLink, type: :model do
  let(:organization) { build(:provider_organization) }
  let(:credentialed_delegate) { build(:user) }
  let(:new_cd_invite) { build(:invitation) }
  let(:cd_org_link) { build(:cd_org_link) }

  it 'has foreign keys' do
    expect(cd_org_link.user_id).to exist
    expect(cd_org_link.provider_organization_id).to exist
    expect(cd_org_link.invitation_id).to exist
  end
end
