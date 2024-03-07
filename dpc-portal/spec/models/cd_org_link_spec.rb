# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CdOrgLink, type: :model do
  it "has foreign keys" do
    cd_org_link = CdOrgLink.new
    expect(cd_org_link.user_id).to exist
    expect(cd_org_link.provider_organization_id).to exist
    expect(cd_org_link.invitation_id).to exist
  end
end
