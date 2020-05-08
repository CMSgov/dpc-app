desc 'Add sandbox id to existing organizations if null'

task :add_sandbox_ids => :environment do
  if ENV['ENV'] == 'prod-sbx'
    Organization.find_each do |o|
      if o.sandbox_id == nil
        o.sandbox_id = Luhnacy.generate(15, :prefix => '808403')[-10..-1]
        o.save!
      end
    end
  end
end