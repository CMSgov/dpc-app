# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Lograge initializer' do
  let(:lograge_config) { Rails.application.config.lograge }

  describe 'configuration' do
    it 'is enabled' do
      expect(lograge_config.enabled).to be true
    end

    it 'uses JSON formatter' do
      expect(lograge_config.formatter).to be_a(Lograge::Formatters::Json)
    end

    it 'ignores health check actions' do
      expect(lograge_config.ignore_actions).to include('HealthCheck::HealthCheckController#index')
    end
  end

  describe 'custom_options lambda' do
    let(:custom_options) { lograge_config.custom_options }
    let(:event) { instance_double(ActiveSupport::Notifications::Event, payload: {}) }

    before do
      allow(CurrentAttributes).to receive(:to_log_hash).and_return({ request_id: 'abc-123' })
    end

    it 'includes the ddsource' do
      result = custom_options.call(event)
      expect(result[:ddsource]).to eq('ruby')
    end

    it 'includes the environment from ENV or defaults to :development' do
      result = custom_options.call(event)
      expected_env = ENV['ENV'] || :development
      expect(result[:environment]).to eq(expected_env)
    end

    it 'includes the log level from ENV or defaults to :info' do
      result = custom_options.call(event)
      expected_level = ENV['LOG_LEVEL'] || :info
      expect(result[:level]).to eq(expected_level)
    end

    it 'includes a time value' do
      result = custom_options.call(event)
      expect(result[:time]).to be_a(Time)
    end

    it 'merges CurrentAttributes log hash' do
      result = custom_options.call(event)
      expect(result[:request_id]).to eq('abc-123')
    end

    context 'when an exception is present in the event payload' do
      let(:exception) { StandardError.new('something broke') }
      let(:event) do
        instance_double(ActiveSupport::Notifications::Event,
                        payload: { exception_object: exception })
      end

      before do
        allow(CurrentAttributes).to receive(:to_log_hash).and_return({ request_id: 'xyz-789' })
      end

      it 'includes the exception class' do
        result = custom_options.call(event)
        expect(result[:exception_class]).to eq(StandardError)
      end

      it 'sets exception_reference to the request_id' do
        result = custom_options.call(event)
        expect(result[:exception_reference]).to eq('xyz-789')
      end
    end

    context 'when no exception is present' do
      it 'does not include exception_class' do
        result = custom_options.call(event)
        expect(result).not_to have_key(:exception_class)
      end

      it 'does not include exception_reference' do
        result = custom_options.call(event)
        expect(result).not_to have_key(:exception_reference)
      end
    end
  end

  describe 'logger configuration' do
    it 'sets a stdout logger when DISABLE_JSON_LOGGER is not true' do
      # The initializer runs at boot — if DISABLE_JSON_LOGGER != 'true' in test env,
      # the logger should have been set
      if ENV['DISABLE_JSON_LOGGER'] == 'true'
        expect(lograge_config.logger).to be_nil
      else
        expect(lograge_config.logger).to be_a(ActiveSupport::Logger)
      end
    end
  end
end
