# frozen_string_literal: true

desc 'Generate fake npis to existing organizations if null'

task generate_npis: :environment do
  if ENV['ENV'] == 'prod-sbx'
    Organization.find_each do |o|
      if o.npi.nil?
        o.npi = LuhnacyLib.generate_npi
        o.save!
      end
    end
  end
end
