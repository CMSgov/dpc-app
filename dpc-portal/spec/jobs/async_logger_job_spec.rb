# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AsyncLoggerJob, type: :job do
  include ActiveJob::TestHelper

  describe :perform do
    it 'should log what receives' do
      params = ['Authorized official has a waiver',
                { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                  actionType: LoggingConstants::ActionType::AoHasWaiver }]
      allow(Rails.logger).to receive(:info)
      expect(Rails.logger).to receive(:info).with(params)
      AsyncLoggerJob.perform_now(:info, params)
    end
    it 'should log at :unknown if level not available' do
      params = ['Authorized official has a waiver',
                { actionContext: LoggingConstants::ActionContext::BatchVerificationCheck,
                  actionType: LoggingConstants::ActionType::AoHasWaiver }]
      allow(Rails.logger).to receive(:unknown)
      expect(Rails.logger).to receive(:unknown).with(params)
      AsyncLoggerJob.perform_now(:foo, params)
    end
  end
end
