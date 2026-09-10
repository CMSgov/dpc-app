# frozen_string_literal: true

require 'rails_helper'

RSpec.describe StructuredLogging do
  include ActiveSupport::Testing::TimeHelpers

  # Create a minimal anonymous controller that includes the concern
  # so tests are isolated from any real controller logic
  let(:controller_class) do
    Class.new do
      include StructuredLogging

      # Simulate ApplicationController's csp_log_context
      def csp_log_context
        { csp: 'logingov' }
      end
    end
  end

  subject(:controller) { controller_class.new }

  # Freeze time so timestamp assertions are deterministic
  around { |example| freeze_time { example.run } }

  describe '#log_event' do
    let(:expected_timestamp) { Time.now.utc.iso8601 }

    context 'with required fields only' do
      it 'logs at the given level with actionContext and timestamp' do
        expect(Rails.logger).to receive(:info) do |(message, payload)|
          expect(message).to eq('Something happened')
          expect(payload[:actionContext]).to eq('Authentication')
          expect(payload[:timestamp]).to eq(expected_timestamp)
          expect(payload[:csp]).to eq('logingov')
        end

        controller.log_event(:info, 'Something happened',
                             action_context: 'Authentication')
      end

      it 'omits actionType when not provided' do
        expect(Rails.logger).to receive(:info) do |(_, payload)|
          expect(payload).not_to have_key(:actionType)
        end

        controller.log_event(:info, 'Something happened',
                             action_context: 'Authentication')
      end
    end

    context 'with all standard fields' do
      it 'includes all provided fields in the payload' do
        expect(Rails.logger).to receive(:warn) do |(message, payload)|
          expect(message).to eq('User not found')
          expect(payload[:actionContext]).to eq('Authentication')
          expect(payload[:actionType]).to eq('CspUserNotFound')
          expect(payload[:user_identifier]).to eq('abc-123-uuid')
          expect(payload[:csp]).to eq('logingov')
          expect(payload[:timestamp]).to eq(expected_timestamp)
        end

        controller.log_event(:warn, 'User not found',
                             action_context: 'Authentication',
                             action_type: 'CspUserNotFound',
                             user_identifier: 'abc-123-uuid')
      end
    end

    context 'with invitation field' do
      it 'includes invitation id in the payload' do
        expect(Rails.logger).to receive(:info) do |(_, payload)|
          expect(payload[:invitation]).to eq(42)
        end

        controller.log_event(:info, 'Invitation flow started',
                             action_context: 'Registration',
                             action_type: 'CdInvitationFlowStarted',
                             invitation: 42)
      end
    end

    context 'with error field' do
      it 'includes error message in the payload' do
        expect(Rails.logger).to receive(:error) do |(_, payload)|
          expect(payload[:error]).to eq('service timeout')
        end

        controller.log_event(:error, 'Service unavailable',
                             action_context: 'Registration',
                             error: 'service timeout')
      end
    end

    context 'with csp_name alias' do
      it 'maps csp_name to csp in the payload' do
        expect(Rails.logger).to receive(:info) do |(_, payload)|
          expect(payload[:csp]).to eq('login_gov')
          expect(payload).not_to have_key(:csp_name)
        end

        controller.log_event(:info, 'User logged in',
                             action_context: 'Authentication',
                             action_type: 'UserLoggedIn',
                             csp_name: 'login_gov')
      end
    end

    context 'with nil/blank optional fields' do
      it 'omits nil fields from the payload' do
        expect(Rails.logger).to receive(:info) do |(_, payload)|
          expect(payload).not_to have_key(:actionType)
          expect(payload).not_to have_key(:user_identifier)
          expect(payload).not_to have_key(:invitation)
          expect(payload).not_to have_key(:error)
        end

        controller.log_event(:info, 'Minimal log',
                             action_context: 'Authentication',
                             action_type: nil,
                             user_identifier: nil,
                             invitation: nil,
                             error: nil)
      end
    end

    context 'with extra/unknown fields' do
      it 'passes unknown extras through to the payload' do
        expect(Rails.logger).to receive(:info) do |(_, payload)|
          expect(payload[:verification_reason]).to eq('expired')
          expect(payload[:custom_field]).to eq('custom_value')
        end

        controller.log_event(:info, 'Custom event',
                             action_context: 'Authentication',
                             verification_reason: 'expired',
                             custom_field: 'custom_value')
      end
    end

    context 'with different log levels' do
      %i[info warn error debug].each do |level|
        it "delegates to Rails.logger.#{level}" do
          expect(Rails.logger).to receive(level)

          controller.log_event(level, 'Test message',
                               action_context: 'Authentication')
        end
      end
    end

    context 'when csp_log_context is empty' do
      let(:controller_class) do
        Class.new do
          include StructuredLogging

          def csp_log_context
            {} # simulates no active CSP session
          end
        end
      end

      it 'logs without a csp key from context' do
        expect(Rails.logger).to receive(:info) do |(_, payload)|
          expect(payload).not_to have_key(:csp)
        end

        controller.log_event(:info, 'No CSP session',
                             action_context: 'Authentication')
      end
    end
  end

  describe '#build_log_payload (private)' do
    let(:expected_timestamp) { Time.now.utc.iso8601 }

    it 'always includes actionContext and timestamp' do
      payload = controller.send(:build_log_payload, 'Authentication', nil, {})

      expect(payload[:actionContext]).to eq('Authentication')
      expect(payload[:timestamp]).to eq(expected_timestamp)
    end

    it 'merges csp_log_context into the payload' do
      payload = controller.send(:build_log_payload, 'Authentication', nil, {})

      expect(payload[:csp]).to eq('logingov')
    end
  end

  describe '#optional_log_fields (private)' do
    it 'prefers csp over csp_name when both provided' do
      result = controller.send(:optional_log_fields, nil,
                               { csp: 'direct_csp', csp_name: 'named_csp' })

      expect(result[:csp]).to eq('direct_csp')
      expect(result).not_to have_key(:csp_name)
    end

    it 'falls back to csp_name when csp is absent' do
      result = controller.send(:optional_log_fields, nil,
                               { csp_name: 'named_csp' })

      expect(result[:csp]).to eq('named_csp')
      expect(result).not_to have_key(:csp_name)
    end

    it 'excludes known keys from remaining extras' do
      result = controller.send(:optional_log_fields, 'SomeType',
                               { user_identifier: 'uid',
                                 invitation: 1,
                                 csp_name: 'logingov',
                                 error: 'oops',
                                 custom: 'value' })

      expect(result).not_to have_key(:csp_name)
      expect(result[:custom]).to eq('value')
    end
  end
end
