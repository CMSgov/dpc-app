# frozen_string_literal: true

class Luhnacy

  def self.generate_npi
    loop do
      npi = Luhnacy.generate(15, prefix: '808403')[-10..-1]
      break npi unless Organization.where(npi: npi).exists?
    end
  end

  def self.validate_npi(npi)
    Luhnacy.doctor_npi?(npi)
  end
end
