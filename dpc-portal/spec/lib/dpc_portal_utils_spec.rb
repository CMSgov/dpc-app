# frozen_string_literal: true

require 'rails_helper'
require 'dpc_portal_utils'
RSpec.describe DpcPortalUtils do
  before { allow(ENV).to receive(:fetch).and_call_original }
  it 'should return localhost' do
    expect(my_protocol_host).to eq 'http://localhost:3100'
  end
  it 'should fetch HOST_NAME from env if not local' do
    expect(ENV).to receive(:fetch).with('ENV', nil).and_return('dev')
    expect(ENV).to receive(:fetch).with('HOST_NAME', nil).and_return('dev.dpc.cms.gov')
    expect(my_protocol_host).to eq 'https://dev.dpc.cms.gov'
  end
end
