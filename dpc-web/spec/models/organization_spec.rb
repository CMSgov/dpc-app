# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Organization, type: :model do
  describe 'api_environments=' do
    it 'rejects non-arrays and sets attribute to []' do
      org = create(:organization, api_environments: 'not_array')
      expect(org.api_environments).to eq([])
    end

    it 'rejects blank items in array' do
      org = create(:organization, api_environments: ['', nil, 1])
      expect(org.api_environments).to eq([1])
    end
  end
end
