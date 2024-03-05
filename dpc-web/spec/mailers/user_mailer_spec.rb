# frozen_string_literal: true

require 'rails_helper'

RSpec.describe UserMailer, type: :mailer do
  describe '#organization_sandbox_email' do
    context 'vendor is true' do
      it 'renders vendor_sandbox_email template' do
        user = build(:user)
        mailer = UserMailer.with(user:, vendor: true).organization_sandbox_email

        expect(mailer.body).to match('data-test="vendor-sandbox-content"')
      end
    end

    context 'vendor is false' do
      it 'renders provider_sandbox_email template' do
        user = build(:user)
        mailer = UserMailer.with(user:, vendor: false).organization_sandbox_email

        expect(mailer.body).to match('data-test="provider-sandbox-content"')
      end
    end
  end
end
