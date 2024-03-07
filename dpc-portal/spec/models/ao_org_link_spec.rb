# frozen_string_literal

require 'rails_helper'

RSpec.describe AoOrgLink, type: :model do
  it "has foreign keys" do
    ao_org_link = AoOrgLink.new
    expect(ao_org_link.user_id).to exist
    expect(ao_org_link.provider_organization_id).to exist
  end
end
