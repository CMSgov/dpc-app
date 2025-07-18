# frozen_string_literal: true

require 'fakeredis/rspec'
require 'simplecov'

require 'axe-rspec'
require 'capybara/rspec'

# Accessibility tests are run separately and should not be subject to simplecov constraints
unless ENV['ACCESSIBILITY'] == 'true'
  SimpleCov.start 'rails' do
    track_files '**/{app,lib}/**/*.rb'

    add_group 'Serializers', 'app/serializers'
    add_group 'Services', 'app/services'
    add_group 'Validators', 'app/validators'

    add_filter 'app/jobs/application_job.rb'
    add_filter 'app/channels/application_cable/connection.rb'
    add_filter 'app/controllers/pages_controller.rb' # loads static content
    add_filter 'vendor'

    SimpleCov.minimum_coverage 90
    SimpleCov.minimum_coverage_by_file 80
  end
end

require 'webmock/rspec'
WebMock.disable_net_connect!(allow_localhost: true, allow: ['github.com', 'objects.githubusercontent.com'])

RSpec.configure do |config|
  config.filter_run_excluding type: :system
  config.expect_with :rspec do |expectations|
    expectations.include_chain_clauses_in_custom_matcher_descriptions = true
  end

  config.mock_with :rspec do |mocks|
    # Prevents you from mocking or stubbing a method that does not exist on
    # a real object. This is generally recommended, and will default to
    # `true` in RSpec 4.
    mocks.verify_partial_doubles = true
    mocks.verify_doubled_constant_names = true
  end

  config.shared_context_metadata_behavior = :apply_to_host_groups

  config.before do
    Truemail.configuration.default_validation_type = :regex
  end

  # The settings below are suggested to provide a good initial experience
  # with RSpec, but feel free to customize to your heart's content.

  # This allows you to limit a spec run to individual examples or groups
  # you care about by tagging them with `:focus` metadata. When nothing
  # is tagged with `:focus`, all examples get run. RSpec also provides
  # aliases for `it`, `describe`, and `context` that include `:focus`
  # metadata: `fit`, `fdescribe` and `fcontext`, respectively.
  # config.filter_run_when_matching :focus

  # Allows RSpec to persist some state between runs in order to support
  # the `--only-failures` and `--next-failure` CLI options. We recommend
  # you configure your source control system to ignore this file.
  # config.example_status_persistence_file_path = "spec/examples.txt"

  # Limits the available syntax to the non-monkey patched syntax that is
  # recommended. For more details, see:
  #   - http://rspec.info/blog/2012/06/rspecs-new-expectation-syntax/
  #   - http://www.teaisaweso.me/blog/2013/05/27/rspecs-new-message-expectation-syntax/
  #   - http://rspec.info/blog/2014/05/notable-changes-in-rspec-3/#zero-monkey-patching-mode
  # config.disable_monkey_patching!

  # Many RSpec users commonly either run the entire suite or an individual
  # file, and it's useful to allow more verbose output when running an
  # individual spec file.
  # if config.files_to_run.one?
  #   Use the documentation formatter for detailed output, unless a formatter
  #   has already been configured (e.g. via a command-line flag).
  #   config.default_formatter = "doc"
  # end

  # Print the 10 slowest examples and example groups at the
  # end of the spec run, to help surface which specs are running
  # particularly slow.
  # config.profile_examples = 10

  # Run specs in random order to surface order dependencies. If you find an
  # order dependency and want to debug it, you can fix the order by providing
  # the seed, which is printed after each run.
  #     --seed 1234
  config.order = :random

  # Seed global randomization in this process using the `--seed` CLI option.
  # Setting this allows you to use `--seed` to deterministically reproduce
  # test failures related to randomization by passing the same `--seed` value
  # as the one that triggered the failure.
  # Kernel.srand config.seed
end
