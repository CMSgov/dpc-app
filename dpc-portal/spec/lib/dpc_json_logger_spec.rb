# frozen_string_literal: true

require 'rails_helper'

RSpec.shared_examples 'logger method' do |log_method, level_name|
  let(:strout) { StringIO.new }
  let(:logger) do
    lgr = DpcJsonLogger.new(strout)
    lgr.formatter = DpcJsonLogger.formatter
    lgr
  end

  describe log_method do
    it "logs #{log_method}" do
      logger.send(log_method, 'This is a test', key: 'value')
      json_result = JSON.parse(strout.string)
      expect(json_result['level']).to eq(level_name)
      expect(json_result['message']).to eq('This is a test')
      expect(json_result['key']).to eq('value')
    end

    it 'calculates message from block if provided' do
      logger.send(log_method) do
        'Calculated message'
      end
      json_result = JSON.parse(strout.string)
      expect(json_result['level']).to eq(level_name)
      expect(json_result['message']).to eq('Calculated message')
    end

    it 'raises error if message is not a string' do
      expect { logger.info(1234).to raise_error(ArgumentError) }
    end
  end
end

RSpec.describe DpcJsonLogger do
  include_examples 'logger method', 'debug', 'DEBUG'
  include_examples 'logger method', 'info', 'INFO'
  include_examples 'logger method', 'warn', 'WARN'
  include_examples 'logger method', 'error', 'ERROR'
  include_examples 'logger method', 'fatal', 'FATAL'
  include_examples 'logger method', 'unknown', 'ANY'
end
