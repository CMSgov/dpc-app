desc 'Add sandbox id to existing organizations if null'

task :add_sandbox_ids do
  if ENV['DEPLOY_ENV'] == 'prod-sbx' do
    Organization.find_each do |o|
      if o.sandbox_id == nil do
        o.sandbox_id = Luhnacy.generate(15, :prefix => '808403')[-10..-1]
        o.save!
      end
    end
  end
end