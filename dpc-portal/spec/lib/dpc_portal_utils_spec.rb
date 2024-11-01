# frozen_string_literal: true

require 'rails_helper'
require 'dpc_portal_utils'
RSpec.describe DpcPortalUtils do
  before { allow(ENV).to receive(:fetch).and_call_original }
  it 'should return localhost' do
    expect(my_protocol_host).to eq 'http://localhost:3100'
  end
  it 'should return sandbox.dpc.cms.gov if prod-sbx' do
    expect(ENV).to receive(:fetch).with('ENV', nil).and_return('prod-sbx')
    expect(my_protocol_host).to eq 'https://sandbox.dpc.cms.gov'
  end
  it 'should return {env}.dpc.cms.gov unless prod-sbx' do
    expect(ENV).to receive(:fetch).with('ENV', nil).and_return('fake_env')
    expect(my_protocol_host).to eq 'https://fake_env.dpc.cms.gov'
  end
end
