# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CdOrgLinkPlaceholder, type: :model do
  it 'sets pending' do
    cd_org_link = CdOrgLinkPlaceholder.new(given_name: 'Bob', family_name: 'Hodges', email: 'bob@example.com', pending: false)
    expect(cd_org_link.pending?).to eq false
  end

  it 'shows attributes' do
    cd_org_link = CdOrgLinkPlaceholder.new(given_name: 'Bob', family_name: 'Hodges', email: 'bob@example.com', pending: false,
                                           verification_code: 'ABC123')
    attrs = cd_org_link.show_attributes
    expect(attrs['full_name']).to eq 'Bob Hodges'
    expect(attrs['email']).to eq 'bob@example.com'
    expect(attrs['verification_code']).to eq 'ABC123'
    expect(attrs['activated_at']).to eq 1.day.ago.to_s
  end
end
