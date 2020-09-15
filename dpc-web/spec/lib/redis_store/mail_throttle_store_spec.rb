# frozen_string_literal: true

require 'rails_helper'
require './lib/redis_store/mail_throttle_store'

describe RedisStore::MailThrottleStore do
  let(:mail_throttler) { described_class.new }
  let(:limit) { 2 }
  let(:expiration) { 5 }

  around(:each) do |spec|
    default_limit = Rails.configuration.x.mail_throttle.limit
    default_expiration = Rails.configuration.x.mail_throttle.expiration
    Rails.configuration.x.mail_throttle.limit = limit
    Rails.configuration.x.mail_throttle.expiration = expiration
    spec.run
    Rails.configuration.x.mail_throttle.limit = default_limit
    Rails.configuration.x.mail_throttle.expiration = default_expiration
  end

  describe '.can_email?' do
    context 'when under the throttle limit' do
      it 'returns true' do
        limit.times do
          expect(mail_throttler.can_email?('test@email.com')).to eq true
        end
      end
    end

    context 'returns false when exceeding the throttle limit' do
      it 'returns false' do
        limit.times do
          mail_throttler.can_email?('test@email.com')
        end

        expect(mail_throttler.can_email?('test@email.com')).to eq false
      end
    end

    context 'when expiration is met' do
      let(:expiration) { 1 }

      it 'returns true' do
        limit.times do
          mail_throttler.can_email?('test@email.com')
        end

        expect(mail_throttler.can_email?('test@email.com')).to eq false
        sleep(expiration + 1)
        expect(mail_throttler.can_email?('test@email.com')).to eq true
      end
    end
  end
end
