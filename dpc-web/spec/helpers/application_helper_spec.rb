# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ApplicationHelper, type: :helper do
  describe '#internal_page?' do
    it 'returns false if not an internal path' do
      stubbed_request = instance_double('ActionDispatch::Request')
      allow(stubbed_request).to receive(:path).and_return('/users/profile')
      allow(helper).to receive(:request).and_return(stubbed_request)

      expect(helper.internal_page?).to eq false
    end

    it 'returns true if an internal path' do
      stubbed_request = instance_double('ActionDispatch::Request')
      allow(stubbed_request).to receive(:path).and_return('/internal/users')
      allow(helper).to receive(:request).and_return(stubbed_request)

      expect(helper.internal_page?).to eq true
    end
  end
end
