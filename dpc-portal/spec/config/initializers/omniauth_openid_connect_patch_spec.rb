# frozen_string_literal: true

require 'rails_helper'

describe OmniAuth::Strategies::OpenIDConnect do
  let(:strategy) { described_class.new(nil) }
  let(:payload) do
    { 'sub' => '12345', 'email' => 'test@example.com' }
  end

  describe '#user_info' do
    context 'when fetch_userinfo_payload succeeds' do
      before do
        allow(strategy).to receive(:fetch_userinfo_payload).and_return(payload)
      end

      it 'returns a UserInfo object' do
        result = strategy.user_info
        expect(result).to be_a(OpenIDConnect::ResponseObject::UserInfo)
      end

      it 'memoizes the result' do
        expect(strategy).to receive(:fetch_userinfo_payload).once
        strategy.user_info
        strategy.user_info
      end
    end

    context 'when fetch_userinfo_payload raises an error' do
      let(:error) { StandardError.new('something went wrong') }

      before do
        allow(strategy).to receive(:fetch_userinfo_payload).and_raise(error)
        allow(Rails.logger).to receive(:error)
        allow(strategy).to receive(:fail!)
      end

      it 'logs the error with the correct context' do
        expect(Rails.logger).to receive(:error).with(
          ['OIDC userinfo processing failed',
           hash_including(
             actionContext: LoggingConstants::ActionContext::Authentication,
             actionType: LoggingConstants::ActionType::OidcUserInfoFailed,
             exceptionClass: 'StandardError'
           )]
        )
        strategy.user_info
      end

      it 'calls fail! with the correct arguments' do
        expect(strategy).to receive(:fail!).with(:user_info_failed, error)
        strategy.user_info
      end

      it 'returns nil' do
        expect(strategy.user_info).to be_nil
      end
    end
  end
end
