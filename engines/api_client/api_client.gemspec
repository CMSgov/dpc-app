# frozen_string_literal: true

require_relative 'lib/api_client/version'

Gem::Specification.new do |spec|
  spec.name        = 'api_client'
  spec.version     = ApiClient::VERSION
  spec.authors     = ['Jeffrey Dettmann']
  spec.email       = ['jeffreydettmann@navapbc.com']
  spec.homepage    = 'https://github.com/CMSgov/dpc-app'
  spec.summary     = 'Contacts dpc api.'
  spec.description = 'Provides functionality for interacting with the dpc api.'
  spec.required_ruby_version = '>= 3.3'

  # Prevent pushing this gem to RubyGems.org. To allow pushes either set the "allowed_push_host"
  # to allow pushing to a single host or delete this section to allow pushing to any host.
  spec.metadata['allowed_push_host'] = "TODO: Set to 'http://mygemserver.com'"
  spec.metadata['rubygems_mfa_required'] = 'true'

  spec.metadata['homepage_uri'] = spec.homepage
  spec.metadata['source_code_uri'] = 'https://github.com/CMSgov/dpc-app'
  spec.metadata['changelog_uri'] = 'https://github.com/CMSgov/dpc-app'

  spec.files = Dir.chdir(File.expand_path(__dir__)) do
    Dir['{app,config,db,lib}/**/*', 'README.md']
  end

  spec.add_dependency 'activemodel', '~> 7.0.8.1'
  spec.add_dependency 'active_model_serializers'
  spec.add_dependency 'fhir_client'
  spec.add_dependency 'macaroons'
end
