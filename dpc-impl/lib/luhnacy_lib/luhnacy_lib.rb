# frozen_string_literal: true

module LuhnacyLib
  def self.generate_npi
    Luhnacy.generate(15, prefix: '808403')[-10..-1]
  end

  def self.validate_npi(npi)
    Luhnacy.doctor_npi?(npi)
  end
end
