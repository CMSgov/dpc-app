# frozen_string_literal: true

require 'rails_helper'

RSpec.describe ProviderOrganization, type: :model do
  describe :validations do
    it 'should pass if it has an npi' do
      expect(ProviderOrganization.new(npi: '1111111111')).to be_valid
    end

    it 'should faile without npi' do
      expect(ProviderOrganization.new).to_not be_valid
    end
  end
end
