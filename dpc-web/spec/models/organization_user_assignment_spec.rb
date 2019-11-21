# frozen_string_literal: true

require 'rails_helper'

RSpec.describe OrganizationUserAssignment, type: :model do
  describe 'callbacks' do
    describe '#send_organization_sandbox_email' do
      it 'sends email if org is sandbox_enabled' do
        org = create(:organization)
        user = create(:user)

        allow(org).to receive(:sandbox_enabled?).and_return(true)
        mailer = double(UserMailer)
        allow(UserMailer).to receive(:with).with(user: user, organization: org).and_return(mailer)
        allow(mailer).to receive(:organization_sandbox_email).and_return(mailer)
        allow(mailer).to receive(:deliver_later)

        create(:organization_user_assignment, organization: org, user: user)

        expect(UserMailer).to have_received(:with)
        expect(mailer).to have_received(:organization_sandbox_email)
        expect(mailer).to have_received(:deliver_later)
      end

      it 'does not send email if org is not sandbox_enabled' do
        org = create(:organization)

        allow(org).to receive(:sandbox_enabled?).and_return(false)
        allow(UserMailer).to receive(:with)

        create(:organization_user_assignment, organization: org)

        expect(UserMailer).not_to have_received(:with)
      end
    end
  end
end
