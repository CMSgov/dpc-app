# frozen_string_literal: true

require 'rails_helper'

RSpec.describe OrganizationUserAssignment, type: :model do
  include DpcClientSupport

  describe 'callbacks' do
    describe '#send_organization_sandbox_email' do
      it 'sends email if org is enabled in API in the prod-sbx environment' do
        allow(ENV).to receive(:[]).and_call_original
        allow(ENV).to receive(:[]).with('ENV').and_return('prod-sbx')
        stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

        org = create(:organization)
        create(:registered_organization, organization: org)
        user = create(:user)

        mailer = double(UserMailer)
        allow(UserMailer).to receive(:with).with(user: user, vendor: false).and_return(mailer)
        allow(mailer).to receive(:organization_sandbox_email).and_return(mailer)
        allow(mailer).to receive(:deliver_later)

        create(:organization_user_assignment, organization: org, user: user)

        expect(UserMailer).to have_received(:with)
        expect(mailer).to have_received(:organization_sandbox_email)
        expect(mailer).to have_received(:deliver_later)
      end

      it 'does not send email if org is not enabled in API in the prod-sbx environment' do
        org = create(:organization)

        allow(UserMailer).to receive(:with)

        create(:organization_user_assignment, organization: org)

        expect(UserMailer).not_to have_received(:with)
      end

      context 'when mail rate limit has been reached' do
        around(:each) do |spec|
          default_limit = Rails.configuration.x.mail_throttle.limit
          Rails.configuration.x.mail_throttle.limit = 0
          spec.run
          Rails.configuration.x.mail_throttle.limit = default_limit
        end

        it 'does not send an email' do
          allow(ENV).to receive(:[]).and_call_original
          allow(ENV).to receive(:[]).with('ENV').and_return('prod-sbx')

          stub_api_client(message: :create_organization, success: true, response: default_org_creation_response)

          org = create(:organization)
          create(:registered_organization, organization: org)
          user = create(:user)

          allow(UserMailer).to receive(:with)

          create(:organization_user_assignment, organization: org, user: user)

          expect(UserMailer).not_to have_received(:with)
        end
      end
    end
  end
end
