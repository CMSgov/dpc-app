# frozen_string_literal: true

require 'rails_helper'

RSpec.describe DpcJsonLogger do
  let(:strout) { StringIO.new }
  let(:logger) do
    lgr = DpcJsonLogger.new(strout)
    lgr.formatter = DpcJsonLogger.formatter
    lgr
  end

  describe :info do
    it 'logs info' do
      logger.info('This is a test', key: 'value')
      json_result = JSON.parse(strout.string)
      expect(json_result['level']).to eq('INFO')
      expect(json_result['message']).to eq('This is a test')
      expect(json_result['key']).to eq('value')
    end

    it 'calculates message from block if provided' do
      logger.info do
        'Calculated message'
      end
      json_result = JSON.parse(strout.string)
      expect(json_result['level']).to eq('INFO')
      expect(json_result['message']).to eq('Calculated message')
    end

    it 'raises error if message is not a string' do
      expect { logger.info(1234).to raise_error(ArgumentError) }
    end
  end

  describe :other_methods do
    it 'logs warning' do
      logger.warn('This is a test', key: 'value')
      json_result = JSON.parse(strout.string)
      expect(json_result['level']).to eq('WARN')
      expect(json_result['message']).to eq('This is a test')
      expect(json_result['key']).to eq('value')
    end

    it 'logs debug' do
      logger.debug('This is a test', key: 'value')
      json_result = JSON.parse(strout.string)
      expect(json_result['level']).to eq('DEBUG')
      expect(json_result['message']).to eq('This is a test')
      expect(json_result['key']).to eq('value')
    end

    it 'logs error' do
      logger.error('This is a test', key: 'value')
      json_result = JSON.parse(strout.string)
      expect(json_result['level']).to eq('ERROR')
      expect(json_result['message']).to eq('This is a test')
      expect(json_result['key']).to eq('value')
    end
  end
end
