# frozen_string_literal: true

require 'rails_helper'
require './lib/luhnacy_lib/luhnacy_lib'

describe LuhnacyLib do
  describe '#generate_npi' do
    it 'generates a valid npi' do
      npi1 = LuhnacyLib.generate_npi
      npi2 = LuhnacyLib.generate_npi
      npi3 = LuhnacyLib.generate_npi

      npi1_check = LuhnacyLib.validate_npi('80840' + npi1)
      npi2_check = LuhnacyLib.validate_npi('80840' + npi2)
      npi3_check = LuhnacyLib.validate_npi('80840' + npi3)

      expect(npi1_check).to eq(true)
      expect(npi2_check).to eq(true)
      expect(npi3_check).to eq(true)
    end
  end
end
